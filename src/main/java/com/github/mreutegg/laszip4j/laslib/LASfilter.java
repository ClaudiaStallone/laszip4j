/*
 * Copyright 2007-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.io.PrintStream;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atof;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atoi;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.util.HashMap;
import java.util.Map;

public class LASfilter {

    private static final PrintStream stderr = System.err;

    private int num_criteria; // unsigned
    private int alloc_criteria; // unsigned
    private LAScriterion[] criteria;
    private int[] counters;

    void clean()
        {
        alloc_criteria = 0;
        num_criteria = 0;
        criteria = null;
        counters = null;
    }

    static void usage()
    {
        fprintf(stderr,"Filter points based on their coordinates.\n");
        fprintf(stderr,"  -keep_tile 631000 4834000 1000 (ll_x ll_y size)\n");
        fprintf(stderr,"  -keep_circle 630250.00 4834750.00 100 (x y radius)\n");
        fprintf(stderr,"  -keep_xy 630000 4834000 631000 4836000 (min_x min_y max_x max_y)\n");
        fprintf(stderr,"  -drop_xy 630000 4834000 631000 4836000 (min_x min_y max_x max_y)\n");
        fprintf(stderr,"  -keep_x 631500.50 631501.00 (min_x max_x)\n");
        fprintf(stderr,"  -drop_x 631500.50 631501.00 (min_x max_x)\n");
        fprintf(stderr,"  -drop_x_below 630000.50 (min_x)\n");
        fprintf(stderr,"  -drop_x_above 630500.50 (max_x)\n");
        fprintf(stderr,"  -keep_y 4834500.25 4834550.25 (min_y max_y)\n");
        fprintf(stderr,"  -drop_y 4834500.25 4834550.25 (min_y max_y)\n");
        fprintf(stderr,"  -drop_y_below 4834500.25 (min_y)\n");
        fprintf(stderr,"  -drop_y_above 4836000.75 (max_y)\n");
        fprintf(stderr,"  -keep_z 11.125 130.725 (min_z max_z)\n");
        fprintf(stderr,"  -drop_z 11.125 130.725 (min_z max_z)\n");
        fprintf(stderr,"  -drop_z_below 11.125 (min_z)\n");
        fprintf(stderr,"  -drop_z_above 130.725 (max_z)\n");
        fprintf(stderr,"  -keep_xyz 620000 4830000 100 621000 4831000 200 (min_x min_y min_z max_x max_y max_z)\n");
        fprintf(stderr,"  -drop_xyz 620000 4830000 100 621000 4831000 200 (min_x min_y min_z max_x max_y max_z)\n");
        fprintf(stderr,"Filter points based on their return number.\n");
        fprintf(stderr,"  -keep_first -first_only -drop_first\n");
        fprintf(stderr,"  -keep_last -last_only -drop_last\n");
        fprintf(stderr,"  -keep_first_of_many -keep_last_of_many\n");
        fprintf(stderr,"  -drop_first_of_many -drop_last_of_many\n");
        fprintf(stderr,"  -keep_middle -drop_middle\n");
        fprintf(stderr,"  -keep_return 1 2 3\n");
        fprintf(stderr,"  -drop_return 3 4\n");
        fprintf(stderr,"  -keep_single -drop_single\n");
        fprintf(stderr,"  -keep_double -drop_double\n");
        fprintf(stderr,"  -keep_triple -drop_triple\n");
        fprintf(stderr,"  -keep_quadruple -drop_quadruple\n");
        fprintf(stderr,"  -keep_quintuple -drop_quintuple\n");
        fprintf(stderr,"Filter points based on the scanline flags.\n");
        fprintf(stderr,"  -drop_scan_direction 0\n");
        fprintf(stderr,"  -keep_scan_direction_change\n");
        fprintf(stderr,"  -keep_edge_of_flight_line\n");
        fprintf(stderr,"Filter points based on their intensity.\n");
        fprintf(stderr,"  -keep_intensity 20 380\n");
        fprintf(stderr,"  -drop_intensity_below 20\n");
        fprintf(stderr,"  -drop_intensity_above 380\n");
        fprintf(stderr,"  -drop_intensity_between 4000 5000\n");
        fprintf(stderr,"Filter points based on classifications or flags.\n");
        fprintf(stderr,"  -keep_class 1 3 7\n");
        fprintf(stderr,"  -drop_class 4 2\n");
        fprintf(stderr,"  -keep_extended_class 43\n");
        fprintf(stderr,"  -drop_extended_class 129 135\n");
        fprintf(stderr,"  -drop_synthetic -keep_synthetic\n");
        fprintf(stderr,"  -drop_keypoint -keep_keypoint\n");
        fprintf(stderr,"  -drop_withheld -keep_withheld\n");
        fprintf(stderr,"  -drop_overlap -keep_overlap\n");
        fprintf(stderr,"Filter points based on their user data.\n");
        fprintf(stderr,"  -keep_user_data 1\n");
        fprintf(stderr,"  -drop_user_data 255\n");
        fprintf(stderr,"  -keep_user_data_below 50\n");
        fprintf(stderr,"  -keep_user_data_above 150\n");
        fprintf(stderr,"  -keep_user_data_between 10 20\n");
        fprintf(stderr,"  -drop_user_data_below 1\n");
        fprintf(stderr,"  -drop_user_data_above 100\n");
        fprintf(stderr,"  -drop_user_data_between 10 40\n");
        fprintf(stderr,"Filter points based on their point source ID.\n");
        fprintf(stderr,"  -keep_point_source 3\n");
        fprintf(stderr,"  -keep_point_source_between 2 6\n");
        fprintf(stderr,"  -drop_point_source 27\n");
        fprintf(stderr,"  -drop_point_source_below 6\n");
        fprintf(stderr,"  -drop_point_source_above 15\n");
        fprintf(stderr,"  -drop_point_source_between 17 21\n");
        fprintf(stderr,"Filter points based on their scan angle.\n");
        fprintf(stderr,"  -keep_scan_angle -15 15\n");
        fprintf(stderr,"  -drop_abs_scan_angle_above 15\n");
        fprintf(stderr,"  -drop_abs_scan_angle_below 1\n");
        fprintf(stderr,"  -drop_scan_angle_below -15\n");
        fprintf(stderr,"  -drop_scan_angle_above 15\n");
        fprintf(stderr,"  -drop_scan_angle_between -25 -23\n");
        fprintf(stderr,"Filter points based on their gps time.\n");
        fprintf(stderr,"  -keep_gps_time 11.125 130.725\n");
        fprintf(stderr,"  -drop_gps_time_below 11.125\n");
        fprintf(stderr,"  -drop_gps_time_above 130.725\n");
        fprintf(stderr,"  -drop_gps_time_between 22.0 48.0\n");
        fprintf(stderr,"Filter points based on their RGB/NIR channel.\n");
        fprintf(stderr,"  -keep_RGB_red 1 1\n");
        fprintf(stderr,"  -keep_RGB_green 30 100\n");
        fprintf(stderr,"  -keep_RGB_blue 0 0\n");
        fprintf(stderr,"  -keep_RGB_nir 64 127\n");
        fprintf(stderr,"Filter points based on their wavepacket.\n");
        fprintf(stderr,"  -keep_wavepacket 0\n");
        fprintf(stderr,"  -drop_wavepacket 3\n");
        fprintf(stderr,"Filter points with simple thinning.\n");
        fprintf(stderr,"  -keep_every_nth 2\n");
        fprintf(stderr,"  -keep_random_fraction 0.1\n");
        fprintf(stderr,"  -thin_with_grid 1.0\n");
        fprintf(stderr,"  -thin_with_time 0.001\n");
        fprintf(stderr,"Boolean combination of filters.\n");
        fprintf(stderr,"  -filter_and\n");
    }

    public boolean parse(int argc, String argv[])
    {
        int i;

        int keep_return_mask = 0; // unsigned
        int drop_return_mask = 0; // unsigned

        int keep_classification_mask = 0; // unsigned
        int drop_classification_mask = 0; // unsigned
    }




public class CommandProcessor {
    private Map<String, CommandAction> commandMap;

    public CommandProcessor() {
        commandMap = new HashMap<>();
        initializeCommands();
    }

    private void initializeCommands() {
        // Aggiungi i comandi supportati e le relative azioni
        commandMap.put("-h", () -> hasHelpOption = true);
        commandMap.put("-help", () -> hasHelpOption = true);
        commandMap.put("-clip_z_below", () -> handleClipZBelowCommand());
        commandMap.put("-clip_z_above", () -> handleClipZAboveCommand());
        commandMap.put("-clip_to_bounding_box", () -> handleClipToBoundingBoxCommand());
        commandMap.put("-clip_to_bb", () -> handleClipToBoundingBoxCommand());
        commandMap.put("-keep_first", () -> handleKeepFirstCommand());
        commandMap.put("-keep_first_of_many", () -> handleKeepFirstOfManyCommand());
        // Aggiungi gli altri comandi e le relative azioni...
    }

    public void processCommands(String[] args) {
        boolean isValid = true;
        int i = 1;

        while (i < args.length) {
            String command = args[i];

            if (command.isEmpty()) {
                i++;
                continue;
            }

            if (commandMap.containsKey(command)) {
                commandMap.get(command).execute();
            } else {
                isValid = false;
                break;
            }

            i++;
        }

        // Gestisci il risultato finale in base al valore di isValid...
    }

    // Definisci le azioni associate ai comandi
    private void handleClipZBelowCommand() {
        // Azione per il comando "-clip_z_below"
    }

    private void handleClipZAboveCommand() {
        // Azione per il comando "-clip_z_above"
    }

    private void handleClipToBoundingBoxCommand() {
        // Azione per il comando "-clip_to_bounding_box" o "-clip_to_bb"
    }

    private void handleKeepFirstCommand() {
        // Azione per il comando "-keep_first"
    }

    private void handleKeepFirstOfManyCommand() {
        // Azione per il comando "-keep_first_of_many"
    }

    // Aggiungi altre azioni per gli altri comandi...

    // Interfaccia per le azioni dei comandi
    private interface CommandAction {
        void execute();
    }
}



        if (drop_return_mask != 0)
        {
            if (keep_return_mask != 0)
            {
                fprintf(stderr,"ERROR: cannot use '-drop_return' and '-keep_return' simultaneously\n");
                return FALSE;
            }
            else
            {
                keep_return_mask = 255 & ~drop_return_mask;
            }
        }
        if (keep_return_mask != 0)
        {
            add_criterion(new LAScriterionKeepReturns(keep_return_mask));
        }

        if (keep_classification_mask != 0)
        {
            if (drop_classification_mask != 0)
            {
                fprintf(stderr,"ERROR: cannot use '-drop_class' and '-keep_class' simultaneously\n");
                return FALSE;
            }
            else
            {
                drop_classification_mask = ~keep_classification_mask;
            }
        }
        if (drop_classification_mask != 0)
        {
            add_criterion(new LAScriterionDropClassifications(drop_classification_mask));
        }

        return TRUE;
    }

    public boolean parse(String string)
    {
        String[] argv = string.split(" ");
        return parse(argv.length, argv);
    }

    int unparse(StringBuilder string)
    {
        int i;
        int n = 0;
        for (i = 0; i < num_criteria; i++)
        {
            n += criteria[i].get_command(string);
        }
        return n;
    }

    int get_decompress_selective()
    {
      int decompress_selective = LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;
      for (int i = 0; i < num_criteria; i++)
      {
        decompress_selective |= criteria[i].get_decompress_selective();
      }
      return decompress_selective;
    }

    void addClipCircle(double x, double y, double radius)
    {
        add_criterion(new LAScriterionKeepCircle(x, y, radius));
    }

    void addClipBox(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z)
    {
        add_criterion(new LAScriterionKeepxyz(min_x, min_y, min_z, max_x, max_y, max_z));
    }

    void addKeepScanDirectionChange()
    {
        add_criterion(new LAScriterionKeepScanDirectionChange());
    }

    public boolean filter(LASpoint point)
    {
        int i;

        for (i = 0; i < num_criteria; i++)
        {
            if (criteria[i].filter(point))
            {
                counters[i]++;
                return TRUE; // point was filtered
            }
        }
        return FALSE; // point survived
    }

    void reset()
    {
        int i;
        for (i = 0; i < num_criteria; i++)
        {
            criteria[i].reset();
        }
    }

    LASfilter()
    {
        alloc_criteria = 0;
        num_criteria = 0;
        criteria = null;
        counters = null;
    }

    void add_criterion(LAScriterion filter_criterion)
    {
        if (num_criteria == alloc_criteria)
        {
            int i;
            alloc_criteria += 16;
            LAScriterion[] temp_criteria = new LAScriterion[alloc_criteria];
            int[] temp_counters = new int[alloc_criteria];
            if (criteria != null)
            {
                for (i = 0; i < num_criteria; i++)
                {
                    temp_criteria[i] = criteria[i];
                    temp_counters[i] = counters[i];
                }
            }
            criteria = temp_criteria;
            counters = temp_counters;
        }
        criteria[num_criteria] = filter_criterion;
        counters[num_criteria] = 0;
        num_criteria++;
    }

    private static boolean asBoolean(int i) {
        return i != 0;
    }

    private static char asChar(String s) {
        if (s != null && !s.isEmpty()) {
            return s.charAt(0);
        } else {
            return '\0';
        }
    }

    public boolean active() {
        return (num_criteria != 0);
    }
}
