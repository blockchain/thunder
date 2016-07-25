/*
 * ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 * Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package network.thunder.core.etc;

import org.slf4j.Logger;
import org.spongycastle.util.Arrays;

public class PerformanceLogger {
    private static final Logger log = Tools.getLogger();

    long time1;
    long time2;
    String title;
    int i = 0;

    public PerformanceLogger () {
        time1 = System.currentTimeMillis();
    }

    public PerformanceLogger (String title) {
        time1 = System.currentTimeMillis();
    }

    public void measure (String event) {
        time2 = System.currentTimeMillis();
        if (Arrays.contains(Constants.LOG_LEVELS, 1)) {
            log.debug((time2 - time1) + "		" + event);
        }
        time1 = System.currentTimeMillis();
    }

    public void measure () {
        i++;
        measure(title + " " + i);

    }
}
