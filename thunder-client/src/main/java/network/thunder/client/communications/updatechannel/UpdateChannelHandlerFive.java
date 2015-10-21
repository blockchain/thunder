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
package network.thunder.client.communications.updatechannel;

import network.thunder.client.communications.objects.UpdateChannelRequestFive;
import network.thunder.client.communications.objects.UpdateChannelResponseFive;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;

import java.sql.Connection;

public class UpdateChannelHandlerFive {
    public Connection conn;
    public Channel channel;

    public void evaluate (UpdateChannelResponseFive m) throws Exception {
        if (channel.getChannelTxClientID() != 0) {
            MySQLConnection.checkKeysFromOtherSide(conn, channel, m.keyList);
        }
        MySQLConnection.getKeysOfUsToBeExposed(conn, channel, true);
    }

    public UpdateChannelRequestFive request () throws Exception {
        UpdateChannelRequestFive request = new UpdateChannelRequestFive();

        request.keyList = MySQLConnection.getKeysOfUsToBeExposed(conn, channel, false);

        return request;
    }
}
