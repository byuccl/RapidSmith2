package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.SiteType;

import java.util.Objects;

/**
 *
 */
public class SiteProperty {
	private SiteType siteType;
	private String propertyName;

	public SiteProperty(SiteType siteType, String propertyName) {
		this.siteType = siteType;
		this.propertyName = propertyName;
	}

	public SiteType getSiteType() {
		return siteType;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SiteProperty that = (SiteProperty) o;
		return siteType == that.siteType &&
				Objects.equals(propertyName, that.propertyName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(siteType, propertyName);
	}

	@Override
	public String toString() {
		return "SiteProperty{" +
				"siteType=" + siteType +
				", propertyName='" + propertyName + '\'' +
				'}';
	}
}
