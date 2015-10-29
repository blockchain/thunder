/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.server.etc;

import org.spongycastle.util.Arrays;

// TODO: Auto-generated Javadoc

/**
 * The Class PerformanceLogger.
 */
public class PerformanceLogger {

    /**
     * The time1.
     */
    long time1;

    /**
     * The time2.
     */
    long time2;

    /**
     * The title.
     */
    String title;

    /**
     * The i.
     */
    int i = 0;

    /**
     * Instantiates a new performance logger.
     */
    public PerformanceLogger () {
        time1 = System.currentTimeMillis();
    }

    /**
     * Instantiates a new performance logger.
     *
     * @param title the title
     */
    public PerformanceLogger (String title) {
        time1 = System.currentTimeMillis();
    }

    /**
     * Measure.
     *
     * @param event the event
     */
    public void measure (String event) {

        time2 = System.currentTimeMillis();
        if (Arrays.contains(Constants.LOG_LEVELS, 1)) {
            System.out.println((time2 - time1) + "		" + event);
        }
        time1 = System.currentTimeMillis();
    }

    /**
     * Measure.
     */
    public void measure () {
        i++;
        measure(title + " " + i);

    }
}
