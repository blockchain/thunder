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

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

public class Constants {

    public static NetworkParameters getNetwork () {
        return TestNet3Params.get();
    }

    public static final int STANDARD_PORT = 2204;
    public static final int ESCAPE_REVOCATION_TIME = 24 * 60 * 60 / 10 * 60; //In blocks..

    //TODO lots of legacy code below from initial server-client architecture
    public static float FEE_PER_BYTE = 3;
    public static float FEE_PER_BYTE_MIN = 0.5f;
    public static float FEE_PER_BYTE_MAX = 15;
    public static int[] LOG_LEVELS = {1, 2, 3, 4, 5};

}
