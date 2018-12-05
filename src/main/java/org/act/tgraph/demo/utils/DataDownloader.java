package org.act.tgraph.demo.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DataDownloader
{
    private static Logger log = LoggerFactory.getLogger("test");

    private static String urlHeader = "http://amitabha.water-crystal.org/TGraphDemo/";
    private static String topoFileName = "Topo.csv.tar.gz";
    private static String[] fileList = new String[]{
            "20101104.tar.gz",
            "20101105.tar.gz",
            "20101106.tar.gz",
            "20101107.tar.gz",
            "20101108.tar.gz" };
    private static long[] fileSize = new long[]{67805801};

    private static File download( String url, File out ) throws IOException {
        if(out.exists() && out.isFile()){
            return out;
        }
        URL website = new URL(url);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(out);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        return out;
    }

    /**
     * Tar文件解压方法
     *
     * @param input
     *            要解压的压缩文件名称（绝对路径名称）
     * @param targetDir
     *            解压后文件放置的路径名（绝对路径名称）
     * @return 解压出的文件列表
     */
    private static List<File> decompressTarGZip( File input, File targetDir ) throws IOException {
        if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
            throw new IOException("failed to create directory " + targetDir);
        }
        List<File> result = new ArrayList<>();
        try (InputStream gzi = new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(input)));
             ArchiveInputStream i = new TarArchiveInputStream(gzi))
        {
            ArchiveEntry entry = null;
            while ((entry = i.getNextEntry()) != null) {
                if (!i.canReadEntryData(entry)) {
                    log.warn("can not read entry");
                    continue;
                }
                File f = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    result.add(f);
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                    }
                }
            }
        }
        return result;
    }


    public static void main(String[] args) throws IOException {

        File f = new File("4.tar.gz");
        log.info("{}",System.getProperty("user.name"), f.length());
//        DataDownloader me = new DataDownloader();
//        me.download(100);
//        me.decompressTarGZip(new File("4.tar.gz"), new File("."));
    }

    public static String getTopo( File tmpDir ) throws IOException
    {
        File topoFile = new File(tmpDir, "Topo.csv");
        if(!topoFile.exists()){
            File out = download( urlHeader + topoFileName, topoFile );
            decompressTarGZip( out, tmpDir );
        }
        return topoFile.getAbsolutePath();
    }

    public static void getTrafficData( String dataPathDir ) throws IOException
    {
        File dataDir = new File(dataPathDir);
        for ( String fileName : fileList )
        {
            File out = download( urlHeader + fileName, new File(dataDir, fileName) );
            decompressTarGZip( out, dataDir );
        }
    }
}
