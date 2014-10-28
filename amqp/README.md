AMQP Services for Tupelo

Codebase organized as a three module Maven build.

objects - Objects, either transmitted natively as Java serialized
objects, or more likely serialized as JSON objects.  Communicated over
the (amqp) wire between client and server programs (via the broker of
course).

client - 'Requesting' programs.  A Tupelo client in this respect is
just a program wanting an 'answer' from some service attached in some
way to a Tupelo store.  An example query might be 'anyone have a disk
containing this md5 hash?'

server - 'Answering' programs.  A Tupelo server in this respect is
just a program willing/able to respond to a request it sees on the
message bus.  A server will need access to one or even many Tupelo
stores.  An example server program is one answering yes/no to the
above example client query.

