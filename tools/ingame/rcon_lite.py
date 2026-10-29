#!/usr/bin/env python3
"""Minimal RCON client for TrueEconomy in-game testing.

Implements the Source RCON protocol (the one Minecraft uses):
packet = length(int32) + id(int32) + type(int32) + body + null end.
Login (type 3) once, then send commands (type 2) and read the
reply (type 0). Auth failure is signalled by id -1.
"""

import socket
import struct

SERVERDATA_AUTH = 3
SERVERDATA_AUTH_RESPONSE = -1
SERVERDATA_EXECCOMMAND = 2
SERVERDATA_RESPONSE_VALUE = 0


class RconError(RuntimeError):
    pass


class RconClient:
    _MAX_BODY = 4096  # Paper caps each RCON reply frame's body at 4096 bytes

    def __init__(self, host, port, timeout=10.0):
        self.host = host
        self.port = port
        self.timeout = timeout
        self.sock = None
        self._id = 0

    def connect(self, password):
        self.sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
        # one auth-response frame with our id signals success; id -1 is auth failure
        _, ptype, pid = self._packet(SERVERDATA_AUTH, password)
        if ptype == SERVERDATA_AUTH_RESPONSE and pid == -1:
            raise RconError("RCON authentication failed (bad password)")
        return self

    def close(self):
        if self.sock:
            try:
                self.sock.close()
            except OSError:
                pass
            self.sock = None

    # -- internals ---------------------------------------------------

    def _packet(self, ptype, body):
        self._id += 1
        payload = struct.pack("<ii", self._id, ptype) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        rid, ptype, text = self._frame()
        if ptype == SERVERDATA_AUTH_RESPONSE and rid == -1:
            raise RconError("RCON authentication failed (bad password)")
        return text, ptype, rid

    def command(self, cmd):
        """Run one console command and return its full output as text."""
        self._id += 1
        payload = (struct.pack("<ii", self._id, SERVERDATA_EXECCOMMAND)
                   + cmd.encode("utf-8") + b"\x00\x00")
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        rid, ptype, text = self._frame()
        # Paper splits long replies into same-id frames whose body is
        # capped at 4096 bytes. Keep reading while the last frame fills
        # the cap; a shorter frame means the reply is finished.
        last_len = len(text)
        while (rid == self._id and ptype == SERVERDATA_RESPONSE_VALUE
               and last_len >= self._MAX_BODY):
            rid, ptype, more = self._frame()
            last_len = len(more)
            text += more
        return text

    def _frame(self):
        (length,) = struct.unpack("<i", self._recv_exact(4))
        rest = self._recv_exact(length)
        rid, ptype = struct.unpack("<ii", rest[:8])
        text = rest[8:-2].decode("utf-8", errors="replace")
        return rid, ptype, text

    def _recv_exact(self, n):
        chunks = []
        while n > 0:
            chunk = self.sock.recv(n)
            if not chunk:
                raise RconError("RCON connection closed unexpectedly")
            chunks.append(chunk)
            n -= len(chunk)
        return b"".join(chunks)
