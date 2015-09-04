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
package network.thunder.client.etc;

import org.spongycastle.util.Arrays;

public class PerformanceLogger {
	long time1;
	long time2;

	int level;

	public PerformanceLogger () {
		time1 = System.currentTimeMillis();
		this.level = 1;
	}

	public PerformanceLogger (int level) {
		time1 = System.currentTimeMillis();
		this.level = level;
	}

	public void measure (String event) {
		time2 = System.currentTimeMillis();
		if (Arrays.contains(Constants.LOG_LEVELS, level)) {
			System.out.println((time2 - time1) + "		" + event);
		}
		time1 = System.currentTimeMillis();
	}
}
