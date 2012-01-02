import socket
from xml.etree import ElementTree as ET

class UtoolConnection:
    def __init__(self, hostname, port):
        self.hostname = hostname
        self.port = port

    def solve(self, domgraph, inputcodec, outputcodec):
        ret = []
        
        self._connect()

        self.f.write("<utool cmd='solve' output-codec='" + outputcodec + "'>\n")
        self.f.write("<usr codec='" + inputcodec + "' string='" + domgraph + "' />\n")
        self.f.write("</utool>\n")
        self.f.flush()
        self.sock.shutdown(1)

        result = self.f.read()
        element = ET.XML(result)

        if element.tag == "error":
            raise Exception("Error: " + element.attrib["explanation"])
        else:
            for subelement in element:
                if subelement.tag == "solution":
                    ret.append(subelement.attrib["string"])

        self._close()

        return ret

    
    def _connect(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setblocking(True)
        self.sock.connect((self.hostname, self.port))
        self.f = self.sock.makefile()
        
    def _close(self):
        self.sock.close()

