/*
 * Created on 03.12.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.dcm4chex.wado.mbean.cache;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IconCacheImpl implements IconCache {

	private static IconCacheImpl singleton = null;
	
	/** the root folder where this cache stores the icon files. */
	private File cacheRoot = null;
	
	/** holds the min cache size. */
	long preferredFreeSpace = 300000000;

	/** holds the max cache size. */
	long minFreeSpace = 200000000;
	
	private IconCacheImpl() {
		
	}
	
	/**
	 * Returns the singleton instance of IconCache.
	 * @return
	 */
	public static IconCache getInstance() {
		if ( singleton == null ) {
			singleton = new IconCacheImpl();
		}
		return singleton;
	}
	
	/**
	 * Get an icon as BufferedImage from cache.
	 * <p>
	 * This method returns the icon from the default path of this cache.<br>
	 * 
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * 
	 * @return	The image if in cache or null.
	 */
	public BufferedImage getIcon(String studyUID, String seriesUID, String instanceUID) {
		return _readJpgFile( getIconFile( studyUID, seriesUID, instanceUID ) );
	}

	/**
	 * Get an icon as file from cache.
	 * <p>
	 * This method returns the icon from the default path of this cache.<br>
	 * 
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * 
	 * @return	The File of the image if in cache or null.
	 */
	public File getIconFile(String studyUID, String seriesUID, String instanceUID) {
		File file = this._getIconFile( "default", studyUID, seriesUID, instanceUID );
		if ( file.exists() ) {
			file.setLastModified( System.currentTimeMillis() ); //set last modified because File has only lastModified timestamp visible.
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Put an icon as BufferedImage to the cache.
	 * <p>
	 * Stores the image on the default path of this cache.
	 * 
	 * @param image			The image of the icon.
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * 
	 * @return The File object of the image in this cache.
	 * 
	 * @throws IOException
	 */
	public File putIcon( BufferedImage image, String studyUID, String seriesUID, String instanceUID) throws IOException {
		File file = this._getIconFile( "default", studyUID, seriesUID, instanceUID );
		_writeImageFile( image, file);
		cleanCache( true );
		return file;
	}

	/**
	 * Get an icon of special size from cache.
	 * <p>
	 * This method use a image size (rows and columns) to search on a special path of this cache.
	 * 
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * @param rows			Image height in pixel.
	 * @param columns		Image width in pixel.
	 * 
	 * @return				The image if in cache or null.
	 */
	public BufferedImage getIcon(String studyUID, String seriesUID, String instanceUID, String rows, String columns) {
		return _readJpgFile( getIconFile( studyUID, seriesUID, instanceUID, rows, columns ) );
	}

	/**
	 * Get an icon of special size from cache.
	 * <p>
	 * This method use a image size (rows and columns) to search on a special path of this cache.
	 * 
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * @param rows			Image height in pixel.
	 * @param columns		Image width in pixel.
	 * 
	 * @return				The File object of the image if in cache or null.
	 */
	public File getIconFile(String studyUID, String seriesUID, String instanceUID, String rows, String columns) {
		File file = this._getIconFile( rows+"-"+columns, studyUID, seriesUID, instanceUID );
		if ( file.exists() ) {
			file.setLastModified( System.currentTimeMillis() ); //set last modified because File has only lastModified timestamp visible.
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Put an icon of special size to this cache.
	 * <p>
	 * Stores the image on a special path of this cache.
	 * 
	 * @param image			The image (with special size)
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * @param rows			Image height in pixel.
	 * @param columns		Image width in pixel.
	 * 
	 * @return The File object of the image in this cache.
	 * @throws IOException
     */
	public File putIcon( BufferedImage image, String studyUID, String seriesUID,
			String instanceUID, String rows, String columns) throws IOException {
		File file = this._getIconFile( rows+"-"+columns, studyUID, seriesUID, instanceUID );
		_writeImageFile( image, file);
		cleanCache( true );
		return file;
	}

	/**
	 * Clears this cache.
	 * <p>
	 * Remove all icons in this cache.
	 */
	public void clearCache() {
		delTree( this.getCacheRoot() );
	}

	/**
	 * Deletes all files in given directory.
	 * <p>
	 * Do nothing if <code>file</code> is not a directory or is <code>null</code>!
	 * 
	 * @param file A directory.
	 */
	public static void delTree(File file) {
		if ( file == null ) return;
		if ( !file.isDirectory() ) return;
		
		File[] files = file.listFiles();
		for ( int i = 0, len = files.length ; i < len ; i++ ) {
			if ( files[i].isFile() ) {
				files[i].delete();
			} else {
				delTree( files[i] );
				files[i].delete();
			}
		}
	}

	/**
	 * Removes old entries to shrink this cache.
	 * <p>
	 * This method can be called to run on the same thread ( e.g. if started via JMX console) or 
	 * in a seperate thread (if clean is handled automatically by IconCache).
	 * <DL>
	 * <DD> 1) check if clean is necessary: <code>getFreeSpace &lt; getMinFreeSpace </code></DD>
	 * <DD> 2) delete the oldest files until <code>getFreeSpace &gt; getPreferredFreeSpace</code></DD>
	 * </DL>
	 * 
	 * @param	background	If true, clean runs in a seperate thread.
	 */
	public void cleanCache( boolean background ) {
		long currFree = getFreeSpace();
		//System.out.println("IconCache.cleancache: free:"+currFree+" minFreeSpace:"+getMinFreeSpace() );
		if ( currFree < getMinFreeSpace() ) {
			final long sizeToDel = getPreferredFreeSpace() - currFree;
			if ( background ) {
		        Thread t = new Thread(new Runnable() {
		            public void run() {
	                	_clean( sizeToDel );
		            }
		        });
		        t.start();
			} else {
            	_clean( sizeToDel );
			}
		} else {
			//System.out.println("IconCache.cleancache: nothing todo" );
		}
	}

	/**
	 * Deletes old files to free the given amount of disk space.
	 * <p>
	 * If a directory is empty after deleting a file, the directory will also be deleted.
	 */
	private void _clean(long sizeToDel ) {
		FileToDelContainer ftd = new FileToDelContainer( this.getCacheRoot(), sizeToDel );
		Iterator iter = ftd.getFilesToDelete().iterator();
		File file;
		while ( iter.hasNext() ) {
			file = (File) iter.next();
			file.delete();
			_deleteDirWhenEmpty( file.getParentFile() );
		}
	}
	
	/**
	 * Deletes the given directory and all parents if they are empty.
	 * 
	 * @param dir Directory
	 */
	private void _deleteDirWhenEmpty( File dir ) {
		if ( dir == null ) return;
		File[] files = dir.listFiles();
		if ( files != null && files.length == 0 ) {
			dir.delete();
			_deleteDirWhenEmpty( dir.getParentFile() );
		}
	}

	/**
	 * Returns the min drive space that must be available on the caches drive.
	 * <p>
	 * This value is used to determine if this cache should be cleaned.
	 * 
	 * @return Min allowed free diskspace in bytes.
	 */
	public long getMinFreeSpace() {
		return this.minFreeSpace;
	}

	/**
	 * Setter of minFreeSpace.
	 * 
	 * @param minFreeSpace The minFreeSpace to set.
	 */
	public void setMinFreeSpace(long minFreeSpace) {
		this.minFreeSpace = minFreeSpace;
	}
	
	/**
	 * Returns the free space that should be remain after cleaning the cache.
	 * <p>
	 * This value is used as lower watermark of the cleaning process.
	 * 
	 * @return Preferred free diskspace in bytes.
	 */
	public long getPreferredFreeSpace() {
		return this.preferredFreeSpace;
	}
	
	/**
	 * Setter of preferredFreeSpace.
	 * 
	 * @param preferredFreeSpace The preferredFreeSpace to set.
	 */
	public void setPreferredFreeSpace(long preferredFreeSpace) {
		this.preferredFreeSpace = preferredFreeSpace;
	}
	
	/**
	 * Returns current free disk space in bytes.
	 * 
	 * @return disk space available on the drive where this cache is stored.
	 */
	public long getFreeSpace() {
		se.mog.io.File file = new se.mog.io.File( getCacheRoot() );
		return file.getDiskSpaceAvailable();
	}

	/**
	 * Returns the root directory of this cache.
	 * <p>
	 * If configured directory doesnt exist, it will be created.
	 * 
	 * @return root directory of this cache.
	 */
	public File getCacheRoot() {
//		 TODO get path from configuration.
		if ( cacheRoot == null ) {
			setCacheRoot( "/iconcache" );
		}
		return cacheRoot;
	}
	
	public void setCacheRoot( String newRoot) {
		if ( newRoot == null ) return;
		if ( cacheRoot != null ) {
			try {
				cacheRoot.renameTo( new File( newRoot ) ); //move only possible if same partition.
			} catch (Throwable t) {
				//TODO copy cache to new dest?
				delTree( cacheRoot );
				cacheRoot.delete();
			}
		}
		cacheRoot = new File( newRoot );
		
		if ( ! cacheRoot.exists() )
			cacheRoot.mkdirs();
		
	}
	
	/**
	 * Returns the File object references the file where the icon is placed within this cache.
	 * <p>
	 * The subdir argument is used to seperate default icons and special sized icons.
	 * <p>
	 * The directory and file names for studyID, seriesID and instaneID are calculated with
	 * <code>_getSubDirName</code>
	 * 
	 * <DL>
	 * <DT>The File object was build like:</DT>
	 * <DD> &lt;root&gt;/[&lt;subdir&gt;/&lt;]studyID&gt;/&lt;seriesID&gt;/&lt;instanceID&gt;</DD>
	 * </DL>
	 * 
	 * @param subdir 		The subdirectory
	 * @param studyUID		Unique identifier of the study.
	 * @param seriesUID		Unique identifier of the series.
	 * @param instanceUID	Unique identifier of the instance.
	 * 
	 * @return
	 */
	private File _getIconFile( String subdir, String studyUID, String seriesUID, String instanceUID ) {
		File file = getCacheRoot();
		if ( subdir != null )
			file = new File( this.getCacheRoot(), subdir ); 
		file = new File( file, _getSubDirName( studyUID ) );
		file = new File( file, _getSubDirName( seriesUID ) );
		file = new File( file, _getSubDirName( instanceUID )+".jpg" ); 
		return file;
	}

	/**
	 * Returns the directory name for given UID.
	 * <p>
	 * This current implementation use the UID as subdirectory name.
	 * 
	 * @param uid	A unique DICOM identifier
	 * 
	 * @return directory name.
	 */
	private String _getSubDirName(String uid) {
		// TODO calculate a shorter name (hash?)
		return uid;
	}

	/**
	 * Writes an image to the given file.
	 * 
	 * @param image	The image.
	 * @param file	The file within this cache to store the image.
	 * 
	 * @throws IOException
	 */
	private void _writeImageFile(BufferedImage bi, File file) throws IOException {
		if ( ! file.getParentFile().exists() ) {
			file.getParentFile().mkdirs();
		}
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		try {
			JPEGImageEncoder enc = JPEGCodec.createJPEGEncoder(out);
			enc.encode(bi);
		}
		finally {
			out.close();
		}
	}
	
	/**
	 * Reads an jpg file into a BufferedImage.
	 * 
	 * @param jpgFile
	 * @return BufferedImage from jpg file.
	 */
	private BufferedImage _readJpgFile(File jpgFile) {
		if ( jpgFile == null ) return null;
		//TODO real work!
		return null;
	}



}
