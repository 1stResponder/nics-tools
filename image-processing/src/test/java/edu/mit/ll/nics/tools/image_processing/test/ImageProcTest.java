/**
 * Copyright (c) 2008-2016, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.tools.image_processing.test;


import java.io.IOException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import edu.mit.ll.nics.tools.image_processing.ImageProcessor;


public class ImageProcTest {
	
	private Logger log = Logger.getLogger(ImageProcTest.class);
	
	
	@Test(testName="Nikon Test", groups={"process"})
	@Parameters({"jpgNikon"})
	public void testProcessNikon(String jpgNikon) {
		
		try {
			Metadata metadata = ImageProcessor.readMetadata(jpgNikon);
						
			Assert.assertNotNull(metadata);
			
			//ImageProcessor.printAllTags(metadata);
			
			Date date = ImageProcessor.getDate(metadata);
			Assert.assertNotNull(date);
			System.out.println("Got Date: " + date);
			
			//Assert.assertEquals(date.getTime(), 1467146069000L);
			
			// TODO:nics-247 May also need to grab the reference for lat and lon to know if you
			// 	need to add a - sign or not?
			
			GeoLocation location = ImageProcessor.getLocation(metadata);			
			Assert.assertNotNull(location);
			
			System.out.println("Lat: " + location.getLatitude());
			Assert.assertEquals(location.getLatitude(), 48.88872633333333);
			
			System.out.println("Lon: " + location.getLongitude());
			Assert.assertEquals(location.getLongitude(), 21.043251166666668);
			
			// TODO:nics-247
			// Altitude... the GPS directory contains an Altitude tag, yet this library's
			// GeoLocation class does not retrieve it. So if we want to include it, have to manually
			// read the GPS->Altitude tag. There are others, too, like lat/lon reference, orientation,
			// and more.

						
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	
	
	@Test(testName="Android Test", groups={"process"})
	@Parameters({"pngAndroid"})
	public void testProcessAndroid(String pngAndroid) {
				
		try {			
			Metadata metadata = ImageProcessor.readMetadata(pngAndroid);
			
			Assert.assertNotNull(metadata);
			
			//ImageProcessor.printAllTags(metadata);
			
			Date date = ImageProcessor.getDate(metadata);
			Assert.assertNotNull(date);			
			System.out.println("Got Android Date: " + date + "(" + date.getTime() + ")");
			
			//Assert.assertEquals(date.getTime(), 1467137285000L);
			
			/* This Android image does not have a location
			GeoLocation location = ImageProcessor.getLocation(metadata);			
			Assert.assertNotNull(location);
			
			System.out.println("Lat: " + location.getLatitude());
			Assert.assertEquals(location.getLatitude(), 48.88872633333333);
			
			System.out.println("Lon: " + location.getLongitude());
			Assert.assertEquals(location.getLongitude(), 21.043251166666668);
			*/
						
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	
	@Test(testName="iPhone 4s", groups={"process"})
	@Parameters({"jpgIPhone4s"})
	public void testProcessIPhone4s(String jpgIPhone4s) {
				
		try {			
			Metadata metadata = ImageProcessor.readMetadata(jpgIPhone4s);
			
			Assert.assertNotNull(metadata);
			
			//ImageProcessor.printAllTags(metadata);
			
			Date date = ImageProcessor.getDate(metadata);
			Assert.assertNotNull(date);			
			System.out.println("Got iPhone4s Date: " + date + "(" + date.getTime() + ")");
			
			//Assert.assertEquals(date.getTime(), 1467231721000L);
			
			/* This image doesn't have GPS
			GeoLocation location = ImageProcessor.getLocation(metadata);			
			Assert.assertNotNull(location);
			
			System.out.println("Lat: " + location.getLatitude());
			Assert.assertEquals(location.getLatitude(), 48.88872633333333);
			
			System.out.println("Lon: " + location.getLongitude());
			Assert.assertEquals(location.getLongitude(), 21.043251166666668);
			*/			
						
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	
	@Test(testName="Canon", groups={"process"})
	@Parameters({"jpgCanon"})
	public void testProcessCanon(String jpgCanon) {
				
		try {			
			Metadata metadata = ImageProcessor.readMetadata(jpgCanon);
			
			Assert.assertNotNull(metadata);
			
			//ImageProcessor.printAllTags(metadata);
			
			Date date = ImageProcessor.getDate(metadata);
			Assert.assertNotNull(date);			
			System.out.println("Got Canon Date: " + date + "(" + date.getTime() + ")");
			
			//Assert.assertEquals(date.getTime(), 1467231820000L);
			
			/* This image does not have GPS
			GeoLocation location = ImageProcessor.getLocation(metadata);			
			Assert.assertNotNull(location);
			
			System.out.println("Lat: " + location.getLatitude());
			Assert.assertEquals(location.getLatitude(), 48.88872633333333);
			
			System.out.println("Lon: " + location.getLongitude());
			Assert.assertEquals(location.getLongitude(), 21.043251166666668);
			*/			
						
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
}
