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
package network.thunder.core.communication;

import com.google.gson.Gson;

import java.sql.Connection;

// TODO: Auto-generated Javadoc

/**
 * The Class Message.
 */
public class Message {

    public String data;
    public int type;

    /**
     * Instantiates a new message.
     *
     * @param o    the o
     * @param type the type
     */
    public Message (Object o, int type) {
        this.fill(o);
        this.type = type;
    }

    /**
     * Instantiates a new message.
     *
     * @param o         the o
     * @param type      the type
     * @param timestamp the timestamp
     */
    public Message (Object o, int type, int timestamp) {
        this.fill(o);
        this.type = type;
    }

    /**
     * Instantiates a new message.
     *
     * @param response the response
     */
    public Message (String response) {
        Message message = new Gson().fromJson(response, Message.class);
        this.data = message.data;
        this.type = message.type;
    }

    /**
     * Fill.
     *
     * @param o the o
     */
    public void fill (Object o) {
        data = new Gson().toJson(o);
    }

    public String getDataString () {
        return new Gson().toJson(this);
    }

    /**
     * Call this method right after receiving it.
     *
     * @param conn the conn
     * @throws Exception the exception
     */
    public void prepare (Connection conn) throws Exception {
        if (this.type == Type.FAILURE) {
            throw new Exception(this.data);
        }
    }

    @Override
    public String toString () {
        return "Message\n\tdata=" + data + "\n\ttype=" + type;
    }

}
