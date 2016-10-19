package edu.byu.ece.rapidSmith.design.subsite;

/**
 * Enumerated Type describing the routing status of a net in the design. 
 * Possible statuses include: <br>
 * 1.) UNROUTED (no sinks have been routed) <br>
 * 2.) PARTIALLY_ROUTED (some sinks have been routed) <br>
 * 3.) FULLY_ROUTED (all sinks have been routed) <br> 
 * 
 * TODO: Add INTRASITE as a possible RouteStatus?
 * 
 * @author Thomas Townsend
 */
public enum RouteStatus {
	UNROUTED,
	PARTIALLY_ROUTED,
	FULLY_ROUTED
}
