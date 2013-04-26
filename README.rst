Cleaner Akka:

* is a Scala cache library based on Akka
* uses JVM direct memory to store cache data in aff-JVM-heap memory,
  this minimizes GC delay
* frees memory immediately when cache entry is removed,
  if sun.misc.Cleaner exists,
  this avoids OutOfMemoryError problem with direct memory
* imitates some of JCache APIs
* cache can be local, remote, or distributed
* Ketama consistent hashing algorithm is used for distributed cache

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

  val cache = akka.cache.createLocal(10 * 1024 * 1024)

Put:

::

  cache.put("John", 83)

Limit of each serialized value is Int.MaxValue, 2 GB.
Limit of the whole cache is MaxDirectMemorySize, see below.

Get:

::

  val future = cache.get("John")
  future.get                      // => Some(83)
  cache.get("Peter").get          // => None

Remove:

::

  cache.remove("John")
  cache.removeAll()

Conditional put:

::

  cache.putIfAbsent("Jack", 73)

Time to live:

::

  cache.put("John", 83, 5)           // Entry will be invalidated after 5 secs
  cache.putIfAbsent("Jack", 73, 10)  // 10 secs

Remote cache
------------

Step 1: On the cache server
~~~~~~~~~~~~~~~~~~~~~~~~~~~

See http://akka.io/docs/akka/1.3-RC4/scala/remote-actors.html to know how to
enable remote mode. For example:

::

  akka.actor.Actor.remote.start("localhost", 2552)

Register a cache with 10 MB limit:

::

  akka.cache.registerRemote("my cache", 10 * 1024 * 1024)

Step 2: On the cache client
~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  val cache = akka.cache.getRemote("my cache", "localhost", 2552)
  cache.put("John", 83)
  cache.get("John").get  // Same as local cache above

Distributed cache
-----------------

Cache will be distributed among multiple nodes using Ketama algoritm.

Step 1: Config nodes to put cache data to in akka-cache.conf
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For example, we config a cluster of 3 nodes:

Node 1:

::

  distributed {
    clientModeOnly = false
    remotes = ["localhost:2553", "localhost:2554"]
  }

Node 2:

::

  distributed {
    clientModeOnly = false
    remotes = ["localhost:2552", "localhost:2554"]
  }

Node 3:

::

  distributed {
    clientModeOnly = false
    remotes = ["localhost:2552", "localhost:2553"]
  }

Step 2: Enable remote mode on all nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See http://akka.io/docs/akka/1.3-RC4/scala/remote-actors.html

Node 1:

::

  akka.actor.Actor.remote.start("localhost", 2552)
  akka.cache.registerRemote("my cache", 10 * 1024 * 1024)

Node 2:

::

  akka.actor.Actor.remote.start("localhost", 2553)
  akka.cache.registerRemote("my cache", 10 * 1024 * 1024)

Node 3:

::

  akka.actor.Actor.remote.start("localhost", 2554)
  akka.cache.registerRemote("my cache", 10 * 1024 * 1024)

Step 3: Access the cache

::

  val cache = akka.cache.getDistributed("my cache", 10 * 1024 * 1024)
  cache.put("John", 83)
  cache.get("John").get  // Same as local and remote cache above

More info
---------

To stop Akka:

::

  import akka.actor.Actors
  Actors.registry.shutdownAll()
  Actors.remote.shutdown()

Good reads
----------

* Off-heap cache: http://www.quora.com/How-does-BigMemory-hide-objects-from-the-Java-garbage-collector
* JCache: https://github.com/jsr107/jsr107spec
* sun.misc.Unsafe: http://stackoverflow.com/questions/5574241/interesting-uses-of-sun-misc-unsafe
* sun.misc.Cleaner: http://groups.google.com/group/netty/browse_thread/thread/3be7f573384af977
* Ketama algorithm: http://www.last.fm/user/RJ/journal/2007/04/10/rz_libketama_-_a_consistent_hashing_algo_for_memcache_clients
* http://stackoverflow.com/questions/8550421/how-to-properly-shutdown-jvm-with-akka-remote-actor
