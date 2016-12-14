package edu.byu.ece.rapidSmith.device.families;

import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.TileType;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface FamilyInfo {
	List<String> generatedFrom();
	List<TileType> tileTypes();
	List<SiteType> siteTypes();
	Set<TileType> clbTiles();
	Set<TileType> switchboxTiles();
	Set<TileType> bramTiles();
	Set<TileType> dspTiles();
	Set<TileType> ioTiles();
	Set<SiteType> sliceSites();
	Set<SiteType> ioSites();
	Set<SiteType> dspSites();
	Set<SiteType> bramSites();

}
