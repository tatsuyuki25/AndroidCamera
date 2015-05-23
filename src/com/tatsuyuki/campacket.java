package com.tatsuyuki;

import java.io.Serializable;

public class campacket implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8180942792593449816L;
	byte[] data;

	public campacket(byte[] d)
	{
		this.data = d;
	}

	public byte[] getData()
	{
		return data;
	}
}
