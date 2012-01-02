from utoolclient import UtoolConnection

conn = UtoolConnection("localhost", 2802)

print conn.solve("[label(x1 f(x2)) label(y1 g(y2)) label(z a) dom(x2 z) dom(y2 z)]", "domcon-oz", "term-prolog")

print conn.solve("[lalala]", "domcon-oz", "term-prolog") # this demonstrates parsing exceptions

