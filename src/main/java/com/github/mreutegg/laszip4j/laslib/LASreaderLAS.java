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

import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;
import com.github.mreutegg.laszip4j.laszip.ByteStreamInFile;
import com.github.mreutegg.laszip4j.laszip.ByteStreamInStream;
import com.github.mreutegg.laszip4j.laszip.LASattribute;
import com.github.mreutegg.laszip4j.laszip.LASindex;
import com.github.mreutegg.laszip4j.laszip.LASitem;
import com.github.mreutegg.laszip4j.laszip.LASreadPoint;
import com.github.mreutegg.laszip4j.laszip.LASzip;
import com.github.mreutegg.laszip4j.laszip.MyDefs;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAS;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAZ;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_NONE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.sizeof;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;

public class LASreaderLAS extends LASreader {

    private static final PrintStream stderr = System.err;

    private RandomAccessFile file;
    private ByteStreamIn stream;
    private LASreadPoint reader;
    private boolean checked_end;

    boolean open(String file_name, int io_buffer_size, boolean peek_only, int decompress_selective)
    {
        if (file_name == null)
        {
            fprintf(stderr,"ERROR: fine name pointer is zero\n");
            return FALSE;
        }

        file = fopenRAF(file_name.toCharArray(), "rb");
        if (file == null)
        {
            fprintf(stderr, "ERROR: cannot open file '%s'\n", file_name);
            return FALSE;
        }

        // create input
        ByteStreamIn in = new ByteStreamInFile(file);

        return open(in, peek_only, decompress_selective);
    }

    boolean open(RandomAccessFile file, boolean peek_only, int decompress_selective)
    {
        if (file == null)
        {
            fprintf(stderr,"ERROR: file pointer is zero\n");
            return FALSE;
        }

        // create input
        ByteStreamIn in = new ByteStreamInFile(file);

        return open(in, decompress_selective);
    }


    public boolean open(InputStream in, int decompress_selective) {
        return open(in, false, decompress_selective);
    }

    boolean open(InputStream stream, boolean peek_only, int decompress_selective)
    {
        // create input
        ByteStreamIn in = new ByteStreamInStream(stream);

        return open(in, peek_only, decompress_selective);
    }

    boolean open(ByteStreamIn stream, int decompress_selective) {
        return open(stream, false, decompress_selective);
    }

   boolean open(ByteStreamIn stream, boolean peek_only, int decompress_selective) {
    int i, j;

    if (stream == null) {
        // Gestione del caso in cui lo stream sia nullo
    }

    header.clean();

    try {
        // Lettura dei valori principali dell'header
        stream.getBytes(header.file_signature, 4);
        header.file_source_ID = stream.get16bitsLE();
        header.global_encoding = stream.get16bitsLE();
        header.project_ID_GUID_data_1 = stream.get32bitsLE();
        header.project_ID_GUID_data_2 = stream.get16bitsLE();
        header.project_ID_GUID_data_3 = stream.get16bitsLE();
        stream.getBytes(header.project_ID_GUID_data_4, 8);
        header.version_major = stream.getByte();
        header.version_minor = stream.getByte();
        stream.getBytes(header.system_identifier, 32);
        stream.getBytes(header.generating_software, 32);
        header.file_creation_day = stream.get16bitsLE();
        header.file_creation_year = stream.get16bitsLE();
        header.header_size = stream.get16bitsLE();
        header.offset_to_point_data = stream.get32bitsLE();
        header.number_of_variable_length_records = stream.get32bitsLE();
        header.point_data_format = stream.getByte();
        header.point_data_record_length = stream.get16bitsLE();
        header.number_of_point_records = stream.get32bitsLE();
        for (i = 0; i < 5; i++) {
            header.number_of_points_by_return[i] = stream.get32bitsLE();
        }
        header.x_scale_factor = longBitsToDouble(stream.get64bitsLE());
        header.y_scale_factor = longBitsToDouble(stream.get64bitsLE());
        header.z_scale_factor = longBitsToDouble(stream.get64bitsLE());
        header.x_offset = longBitsToDouble(stream.get64bitsLE());
        header.y_offset = longBitsToDouble(stream.get64bitsLE());
        header.z_offset = longBitsToDouble(stream.get64bitsLE());
        header.max_x = longBitsToDouble(stream.get64bitsLE());
        header.min_x = longBitsToDouble(stream.get64bitsLE());
        header.max_y = longBitsToDouble(stream.get64bitsLE());
        header.min_y = longBitsToDouble(stream.get64bitsLE());
        header.max_z = longBitsToDouble(stream.get64bitsLE());
        header.min_z = longBitsToDouble(stream.get64bitsLE());

        // Check core header contents
        if (!header.check()) {
            // Gestione dell'header non valido
        }

        // Special handling for LAS 1.3
        if ((header.version_major == 1) && (header.version_minor >= 3) && (header.header_size >= 235)) {
            try {
                header.start_of_waveform_data_packet_record = stream.get64bitsLE();
            } catch (Exception e) {
                // Gestione dell'eccezione
            }
        }

        // Special handling for LAS 1.4
        if ((header.version_major == 1) && (header.version_minor >= 4) && (header.header_size >= 375)) {
            try {
                header.start_of_first_extended_variable_length_record = stream.get64bitsLE();
                header.number_of_extended_variable_length_records = stream.get32bitsLE();
                header.extended_number_of_point_records = stream.get64bitsLE();
                for (i = 0; i < 15; i++) {
                    header.extended_number_of_points_by_return[i] = stream.get64bitsLE();
                }
            } catch (Exception e) {
                // Gestione dell'eccezione
            }
        }

        if (header.user_data_in_header_size != 0) {
            header.user_data_in_header = new byte[header.user_data_in_header_size];
            try {
                stream.getBytes(header.user_data_in_header, header.user_data_in_header_size);
            } catch (Exception e) {
                // Gestione dell'eccezione
            }
        }

        if (peek_only) {
            // Logica per la modalità "peek_only"
        }

        int vlrs_size = 0;

        if (header.number_of_variable_length_records != 0) {
            header.vlrs = new LASvlr[header.number_of_variable_length_records];

            for (i = 0; i < header.number_of_variable_length_records; i++) {
                header.vlrs[i] = new LASvlr();

                if (((int) header.offset_to_point_data - vlrs_size - header.header_size) >= 54) {
                    try {
                        header.vlrs[i].reserved = stream.get16bitsLE();
                        stream.getBytes(header.vlrs[i].user_id, 16);
                        header.vlrs[i].record_id = stream.get16bitsLE();
                        header.vlrs[i].record_length_after_header = stream.get16bitsLE();
                        stream.getBytes(header.vlrs[i].description, 32);

                        if (((int) header.offset_to_point_data - vlrs_size - header.header_size) >= header.vlrs[i].record_length_after_header
                                && header.vlrs[i].record_length_after_header != 0) {
                            if (strcmp(header.vlrs[i].user_id, "laszip encoded") == 0) {
                                header.laszip = new LASzip();
                                header.laszip.compressor = stream.get16bitsLE();
                                header.laszip.coder = stream.get16bitsLE();
                                header.laszip.version_major = stream.getByte();
                                header.laszip.version_minor = stream.getByte();
                                header.laszip.version_revision = stream.get16bitsLE();
                                header.laszip.options = stream.get32bitsLE();
                                header.laszip.chunk_size = stream.get32bitsLE();
                                header.laszip.number_of_special_evlrs = stream.get64bitsLE();
                                header.laszip.offset_to_special_evlrs = stream.get64bitsLE();
                                header.laszip.num_items = stream.get16bitsLE();
                                for (j = 0; j < header.laszip.num_items; j++) {
                                    type = stream.get16bitsLE();
                                    size = stream.get16bitsLE();
                                    version = stream.get16bitsLE();
                                }
                            } else if (((strcmp(header.vlrs[i].user_id, "LAStools") == 0)
                                    && (header.vlrs[i].record_id == 10))
                                    || (strcmp(header.vlrs[i].user_id, "lastools tile") == 0)) {
                                if (header.vlrs[i].record_length_after_header == 28) {
                                    header.vlr_lastiling.level = ... // Processo vlr_lastiling
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Gestione dell'eccezione
                    }
                }
            }
        }

        // Resto del codice rimane invariato

    } catch (Exception e) {
        // Gestione dell'eccezione principale
    }
}
