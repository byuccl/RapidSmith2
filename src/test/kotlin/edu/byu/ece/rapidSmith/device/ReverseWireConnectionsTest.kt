package edu.byu.ece.rapidSmith.device

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests that all forward connections have a reverse connection.
 */
class ReverseWireConnectionsTest {
	@Test fun testV6TileWires() {
		val device = Device.getInstance("xc6vlx75tff484");
		for (tile in device.tileMap.values) {
			for (source in tile.wires) {
				for (sinkConn in source.wireConnections) {
					val sink = sinkConn.sinkWire

					val rconns = sink.reverseWireConnections
					val sinkSources = rconns.filter { it.sinkWire == source }

					assert(sinkSources.size < 2) { "Multiple reverse connections found $sink -> $source" }
					assert(sinkSources.isNotEmpty()) { "No reverse connections found $sink -> $source" }

					val sinkSource = sinkSources[0]
					assertEquals(sinkConn.isPip, sinkSource.isPip)
				}
			}

			for (sink in tile.wires) {
				for (sourceConn in sink.reverseWireConnections) {
					val source = sourceConn.sinkWire

					val conns = source.wireConnections
					val sourceSinks = conns.filter { it.sinkWire == sink }

					assert(sourceSinks.size < 2) { "Multiple reverse connections found $source -> $sink" }
					assert(sourceSinks.isNotEmpty()) { "No reverse connections found $source -> $sink" }

					val sourceSink = sourceSinks[0]
					assertEquals(sourceConn.isPip, sourceSink.isPip)
				}
			}
		}
	}

	@Test fun testV6SiteWires() {
		val device = Device.getInstance("xc6vlx75tff484");
		for (site in device.sites.values) {
			for (type in site.possibleTypes) {
				site.type = type
				for (source in site.wires) {
					for (sinkConn in source.wireConnections) {
						val sink = sinkConn.sinkWire

						val rconns = sink.reverseWireConnections
						val sinkSources = rconns.filter { it.sinkWire == source }

						assert(sinkSources.size < 2) { "Multiple reverse connections found $sink -> $source" }
						assert(sinkSources.isNotEmpty()) { "No reverse connections found $sink -> $source" }

						val sinkSource = sinkSources[0]
						assertEquals(sinkConn.isPip, sinkSource.isPip)
					}
				}
				for (sink in site.wires) {
					for (sourceConn in sink.reverseWireConnections) {
						val source = sourceConn.sinkWire

						val conns = source.wireConnections
						val sourceSinks = conns.filter { it.sinkWire == sink }

						assert(sourceSinks.size < 2) { "Multiple reverse connections found $source -> $sink" }
						assert(sourceSinks.isNotEmpty()) { "No reverse connections found $source -> $sink" }

						val sourceSink = sourceSinks[0]
						assertEquals(sourceConn.isPip, sourceSink.isPip)
					}
				}
			}
		}
	}
}
