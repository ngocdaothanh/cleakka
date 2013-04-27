Cleakka (Cleaner + Akka):

* is a memory cache based on sun.misc.Cleaner and Akka
* uses JVM direct memory to store cache data in off-heap memory;
  this minimizes GC delay
* frees memory immediately when cache entry is removed,
  if sun.misc.Cleaner exists;
  this avoids OutOfMemoryError problem with direct memory
* cache can be local, remote (client-server model), or distributed
  (not clustered, because cache servers do not need to be connected together);
  distributed cache uses `consistent hashing <http://en.wikipedia.org/wiki/Consistent_hashing>`_

Config MaxDirectMemorySize
--------------------------

Amount of direct memory that can be allocated is limited by JVM option
-XX:MaxDirectMemorySize, which by default has relatively low size of 64 MB.
To increase, add setting like this when starting JVM:

::

  -XX:MaxDirectMemorySize=512m

Otherwise you are likely to hit an OutOfMemoryError when using large cache.

Local cache
-----------

Create a cache with 10 MB limit:

::

  val cache = new cleakka.LocalCache(10)

Put:

::

  cache.put(any, anyRef)

You can put anything serializable to the cache.
`Twitter Chill <https://github.com/twitter/chill>`_, which is based on
`Kryo <https://code.google.com/p/kryo/>`_ is used for (de)serializing.

Get:

::

  val opt: Option[MyType] = cache.get[MyType](key)

Remove:

::

  cache.remove(key)
  cache.removeAll()

Conditional put:

::

  cache.putIfAbsent(key, value)

Time to live:

::

  cache.put(key, value, 5)           // 5 secs
  cache.putIfAbsent(key, value, 10)  // 10 secs

Remote cache
------------

Start a CacheServer on a node and access it from another node.

See http://doc.akka.io/docs/akka/2.1.2/scala/remoting.html to know how to
enable Akka remoting.

On cache server node:

::

  cleakka.CacheServer.start("myCache", 10)  // 10 MB cache

On cache client node:

::

  val client = cleakka.CacheClient.connect("myCache", "localhost", 2552)

To connect to the cache server on the current node:

::

  val client = cleakka.CacheClient.connect("myCache")

The client APIs are similar to those of local cache. The only different is the
results are ``scala.concurrent.Future``s:

::

  client.put(key, value)
  val future: Future[MyType] = client.get[MyType](key)

To stop the server:

::

  client.stopServer()

Distributed cache
-----------------

Cache client uses `consistent hashing <http://en.wikipedia.org/wiki/Consistent_hashing>`
algorithm to decide which node to put cache entries.

On cache server nodes, start CacheServers as with remote cache.

On cache client node:

::

  val client = cleakka.ConsistentHashingCacheClient.connect("myCache", Seq(
    ("localhost", 2553),
    ("localhost", 2554)
  ))

Then you can access the distributed cache just like with remote cache.

You should listen to Akka node up and down events to update the server list:

::

  client.removeServer("localhost", 2553)
  client.addServer("localhost", 2555)

References
----------

* Off-heap cache: http://www.quora.com/How-does-BigMemory-hide-objects-from-the-Java-garbage-collector
* JCache: https://github.com/jsr107/jsr107spec
* sun.misc.Unsafe: http://stackoverflow.com/questions/5574241/interesting-uses-of-sun-misc-unsafe
* sun.misc.Cleaner: http://groups.google.com/group/netty/browse_thread/thread/3be7f573384af977
* Ketama algorithm: http://www.last.fm/user/RJ/journal/2007/04/10/rz_libketama_-_a_consistent_hashing_algo_for_memcache_clients
