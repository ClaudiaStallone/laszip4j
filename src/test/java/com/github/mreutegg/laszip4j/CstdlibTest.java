/*
 * Copyright 2021 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j;

import com.github.mreutegg.laszip4j.clib.Cstdlib;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CstdlibTest {

    @Test
    public void srand() {
        int first = Cstdlib.rand();
        int second = Cstdlib.rand();
        Cstdlib.srand(1);
        assertEquals(first, Cstdlib.rand());
        assertEquals(second, Cstdlib.rand());
    }
}
