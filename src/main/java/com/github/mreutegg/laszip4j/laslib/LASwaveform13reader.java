/*
 * Copyright 2007-2012, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.ArithmeticDecoder;
import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;
import com.github.mreutegg.laszip4j.laszip.ByteStreamInFile;
import com.github.mreutegg.laszip4j.laszip.IntegerCompressor;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strlen;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwaveform13reader {

    private static final PrintStream stderr = logger.log;
    
    public int Nbits; // unsigned
    public int Nsamples; // unsigned
    public int temporal; // unsigned
    public float location;
    public float[] XYZt = new float[3];
    public double[] XYZreturn = new double[3];

    public double[] XYZsample = new double[3];

    public int s_count; // unsigned
    public int sample; // unsigned

    public int sampleMin; // unsigned
    public int sampleMax; // unsigned

    public byte[] samples;

    private boolean compressed;
    private int size; // unsigned
    private LASvlr_wave_packet_descr[] wave_packet_descr;
    private RandomAccessFile file;
    private ByteStreamIn stream;
    private long start_of_waveform_data_packet_record;
    private long last_position;
    private ArithmeticDecoder dec;
    private IntegerCompressor ic8;
    private IntegerCompressor ic16;

    public LASwaveform13reader() {
        nbits = 0;
        nsamples = 0;
        temporal = 0;
        location = 0;
        XYZt[0] = XYZt[1] = XYZt[2] = 0;
        XYZreturn[0] = XYZreturn[1] = XYZreturn[2] = 0;

        s_count = 0;
        XYZsample[0] = XYZsample[1] = XYZsample[2] = 0;
        sample = 0;

        sampleMin = 0;
        sampleMax = 0;

        samples = null;

        size = 0;
        compressed = FALSE;
        wave_packet_descr = null;
        file = null;
        stream = null;
        last_position = 0;
        start_of_waveform_data_packet_record = 0;
        dec = null;
        ic8 = null;
        ic16 = null;
    }

    public boolean is_compressed()
    {
        return compressed;
    }

   boolean open(String file_name, long start_of_waveform_data_packet_record, LASvlr_wave_packet_descr[] wave_packet_descr) {
    if (file_name == null || wave_packet_descr == null) {
        fprintf(stderr, "ERROR: file name pointer or wave packet descriptor pointer is null\n");
        return false;
    }

    compressed = false;

    for (LASvlr_wave_packet_descr packet_descr : wave_packet_descr) {
        if (packet_descr != null && packet_descr.getCompressionType() > 0) {
            compressed = true;
            break;
        }
    }

    String lowercaseFileName = file_name.toLowerCase();
    boolean isWdpFile = lowercaseFileName.endsWith(".wdp");
    boolean isWdzFile = lowercaseFileName.endsWith(".wdz");

    if (start_of_waveform_data_packet_record == 0) {
        if (!compressed && (isWdpFile || isWdzFile)) {
            file = fopenRAF(file_name.toCharArray(), "rb");
        } else {
            char[] file_name_temp = file_name.toCharArray();
            int len = strlen(file_name_temp);
            if ((file_name_temp[len - 3] == 'L') || (file_name_temp[len - 3] == 'W')) {
                file_name_temp[len - 3] = 'W';
                file_name_temp[len - 2] = 'D';
                file_name_temp[len - 1] = (compressed ? 'Z' : 'P');
            } else {
                file_name_temp[len - 3] = 'w';
                file_name_temp[len - 2] = 'd';
                file_name_temp[len - 1] = (compressed ? 'z' : 'p');
            }
            file = fopenRAF(file_name_temp, "rb");
        }
    } else {
        file = fopenRAF(file_name.toCharArray(), "rb");
    }

    if (file == null) {
        fprintf(stderr, "ERROR: cannot open waveform file '%s'\n", file_name);
        return false;
    }

    stream = new ByteStreamInFile(file);

    this.start_of_waveform_data_packet_record = start_of_waveform_data_packet_record;
    this.wave_packet_descr = wave_packet_descr;

    long position = start_of_waveform_data_packet_record + 60;
    stream.seek(position);

    byte[] magic = new byte[25];
    try {
        stream.getBytes(magic, 24);
    } catch (Exception e) {
        fprintf(stderr, "ERROR: reading waveform descriptor cross-check\n");
        return false;
    }

    if (strncmp(stringFromByteArray(magic), "LAStools waveform ", 18) == 0) {
        int number; // unsigned
        try {
            number = stream.get16bitsLE();
        } catch (Exception e) {
            fprintf(stderr, "ERROR: reading number of waveform descriptors\n");
            return false;
        }
        for (int i = 0; i < number; i++) {
            int index; // unsigned
            try {
                index = stream.get16bitsLE();
            } catch (Exception e) {
                fprintf(stderr, "ERROR: reading index of waveform descriptor %d\n", i);
                return false;
            }
            if (index > 255) {
                fprintf(stderr, "ERROR: cross-check - index %d of waveform descriptor %d out-of-range\n", index, i);
                return false;
            }
            if (wave_packet_descr[index] == null) {
                fprintf(stderr, "WARNING: cross-check - waveform descriptor %d with index %d unknown\n", i, index);
                int dummy;
                try { dummy = stream.get32bitsLE(); } catch(Exception e)
                {
                    fprintf(stderr,"ERROR: cross-check - reading rest of waveform descriptor %d\n", i);
                    return FALSE;
                }
                continue;
            }
            byte[] compression = new byte[1];
            try { stream.getBytes(compression, 1); } catch(Exception e)
            {
                fprintf(stderr,"ERROR: reading compression of waveform descriptor %d\n", i);
                return FALSE;
            }
            if (compression[0] != wave_packet_descr[index].getCompressionType())
            {
                fprintf(stderr,"ERROR: cross-check - compression %d %d of waveform descriptor %d with index %d is different\n", compression[0], wave_packet_descr[index].getCompressionType(), i, index);
                return FALSE;
            }
            byte[] Nbits = new byte[1];
            try { stream.getBytes(nbits, 1); } catch(Exception e)
            {
                fprintf(stderr,"ERROR: reading nbits of waveform descriptor %d\n", i);
                return FALSE;
            }
            if (nbits[0] != wave_packet_descr[index].getBitsPerSample())
            {
                fprintf(stderr,"ERROR: cross-check - nbits %d %d of waveform descriptor %d with index %d is different\n", nbits[0], wave_packet_descr[index].getBitsPerSample(), i, index);
                return FALSE;
            }
            int nsamples; // unsigned
            try { nsamples = stream.get16bitsLE(); } catch(Exception e)
            {
                fprintf(stderr,"ERROR: reading nsamples of waveform descriptor %d\n", i);
                return FALSE;
            }
            if (nsamples != wave_packet_descr[index].getNumberOfSamples())
            {
                fprintf(stderr,"ERROR: cross-check - nsamples %d %d of waveform descriptor %d with index %d is different\n", nsamples, wave_packet_descr[index].getNumberOfSamples(), i, index);
                return FALSE;
            }
        }
    }

    last_position = stream.tell();

    if (compressed) {
        if (dec == null) dec = new ArithmeticDecoder();
        if (ic8 == null) ic8 = new IntegerCompressor(dec, 8);
        if (ic16 == null) ic16 = new IntegerCompressor(dec, 16);
    }

    return true;
}


    public boolean read_waveform(LASpoint point)
    {
        int index = point.getWavepacketDescriptorIndex(); // unsigned
        if (index == 0)
        {
            return FALSE;
        }

        if (wave_packet_descr[index] == null)
        {
            fprintf(stderr, "ERROR: wavepacket is indexing non-existant descriptor %d\n", index);
            return FALSE;
        }

        nbits = wave_packet_descr[index].getBitsPerSample();
        if ((nbits != 8) && (nbits != 16))
        {
            fprintf(stderr, "ERROR: waveform with %d bits per samples not supported yet\n", nbits);
            return FALSE;
        }

        nsamples = wave_packet_descr[index].getNumberOfSamples();

        //  temporary Optech Fix
        //  nsamples = point.wavepacket.getSize();
        //  if (nbits == 16) nsamples / 2;

        if (nsamples == 0)
        {
            fprintf(stderr, "ERROR: waveform has no samples\n");
            return FALSE;
        }

        temporal = wave_packet_descr[index].getTemporalSpacing();
        location = point.getWavepacketReturnPointWaveformLocation();

        XYZt[0] = point.getWavepacketParametricDx();
        XYZt[1] = point.getWavepacketParametricDy();
        XYZt[2] = point.getWavepacketParametricDz();

        XYZreturn[0] = point.get_x();
        XYZreturn[1] = point.get_y();
        XYZreturn[2] = point.get_z();

        // alloc data

        if (size < ((nbits/8) * nsamples))
        {
            samples = new byte[((nbits/8) * nsamples)];
        }

        size = ((nbits/8) * nsamples);

        // read waveform

        long position = start_of_waveform_data_packet_record + point.getWavepacketOffsetToWaveformData();
        stream.seek(position);

        if (wave_packet_descr[index].getCompressionType() == 0)
        {
            try { stream.getBytes(samples, size); } catch(Exception e)
            {
                fprintf(stderr, "ERROR: cannot read %d bytes for waveform with %d samples of %d bits\n", size, nsamples, nbits);
                return FALSE;
            }
        }
        else
        {
            if (nbits == 8)
            {
                stream.getBytes(samples, 1);
                dec.init(stream);
                ic8.initDecompressor();
                for (s_count = 1; s_count < nsamples; s_count++)
                {
                    samples[s_count] = (byte) ic8.decompress(samples[s_count-1]);
                }
            }
            else
            {
                stream.getBytes(samples, 2);
                dec.init(stream);
                ic16.initDecompressor();
                ByteBuffer bb = ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN);
                for (s_count = 1; s_count < nsamples; s_count++)
                {
                    bb.putChar(s_count*2, (char) ic16.decompress(bb.getChar((s_count-1)*2)));
                }
            }
            dec.done();
        }

        s_count = 0;
        return TRUE;
    }

    boolean get_samples()
    {
        if (nbits == 8)
        {
            sampleMin = samples[0];
            sampleMax = samples[0];
            for (s_count = 1; s_count < nsamples; s_count++)
            {
                if (samples[s_count] < sampleMin) sampleMin = samples[s_count];
                else if (samples[s_count] > sampleMax) sampleMax = samples[s_count];
            }
        }
        else
        {
            ByteBuffer bb = ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN);
            sampleMin = bb.getChar(0);
            sampleMax = bb.getChar(0);
            for (s_count = 1; s_count < nsamples; s_count++)
            {
                char s = bb.getChar(s_count*2);
                sampleMin = Math.min(sampleMin, s);
                sampleMax = Math.max(sampleMax, s);
            }
        }
        s_count = 0;
        return (s_count < nsamples);
    }

    boolean has_samples()
    {
        if (s_count < nsamples)
        {
            if (nbits == 8)
            {
                sample = samples[s_count];
            }
            else
            {
                ByteBuffer bb = ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN);
                sample = bb.getChar(s_count*2);
            }
            s_count++;
            return TRUE;
        }
        return FALSE;
    }

    boolean get_samples_xyz()
    {
        if (nbits == 8)
        {
            sampleMin = samples[0];
            sampleMax = samples[0];
            for (s_count = 1; s_count < nsamples; s_count++)
            {
                if (samples[s_count] < sampleMin) sampleMin = samples[s_count];
                else if (samples[s_count] > sampleMax) sampleMax = samples[s_count];
            }
        }
        else
        {
            ByteBuffer bb = ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN);
            sampleMin = bb.getChar(0);
            sampleMax = bb.getChar(0);
            for (s_count = 1; s_count < nsamples; s_count++)
            {
                char s = bb.getChar(s_count*2);
                sampleMin = Math.min(sampleMin, s);
                sampleMax = Math.max(sampleMax, s);
            }
        }
        s_count = 0;
        return (s_count < nsamples);
    }

    boolean has_samples_xyz()
    {
        if (s_count < nsamples)
        {
            float dist = location - s_count*temporal;
            XYZsample[0] = XYZreturn[0] + dist*XYZt[0];
            XYZsample[1] = XYZreturn[1] + dist*XYZt[1];
            XYZsample[2] = XYZreturn[2] + dist*XYZt[2];
            if (nbits == 8)
            {
                sample = samples[s_count];
            }
            else
            {
                ByteBuffer bb = ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN);
                sample = bb.getChar(s_count * 2);
            }
            s_count++;
            return TRUE;
        }
        return FALSE;
    }

    void close()
    {
        if (stream != null)
        {
            fclose(stream);
            stream = null;
        }
        if (file != null)
        {
            fclose(file);
            file = null;
        }
    }

    private static boolean strstr(String s1, String s2) {
        return s1.contains(s2);
    }
}
