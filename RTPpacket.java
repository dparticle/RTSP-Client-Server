//class RTPpacket

import java.util.*;

public class RTPpacket{

    //size of the RTP header:
//    static int HEADER_SIZE = 12;
    static int HEADER_SIZE = 24;

    //Fields that compose the RTP header
    public int Version;
    public int Padding;
    public int Extension;
    public int CC;
    public int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public int TimeStamp;
    public int Ssrc;

    //Fields that corresponds to extension header
    public int FrameLength;
    public int FrameId;
    public int PktId;
    public int PktEnd;

    //Bitstream of the RTP header
    public byte[] header;

    //size of the RTP payload
    public int payload_size;
    //Bitstream of the RTP payload
    public byte[] payload;

    //--------------------------
    //Constructor of an RTPpacket object from header fields and payload bitstream
    //--------------------------
    public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length,
                     int frame_length, int frame_id, int pkt_id, int pkt_end){
        //fill by default header fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 1337;    // Identifies the server

        //fill changing header fields:
        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;

        //fill extension header fields:
        FrameLength = frame_length;
        FrameId = frame_id;
        PktId = pkt_id;
        PktEnd = pkt_end;

        //build the header bistream:
        header = new byte[HEADER_SIZE];

        //fill the header array of byte with RTP header fields
        header[0] = (byte)(Version << 6 | Padding << 5 | Extension << 4 | CC);
        header[1] = (byte)(Marker << 7 | PayloadType & 0x000000FF);
        header[2] = (byte)(SequenceNumber >> 8);
        header[3] = (byte)(SequenceNumber & 0xFF);
        header[4] = (byte)(TimeStamp >> 24);
        header[5] = (byte)(TimeStamp >> 16);
        header[6] = (byte)(TimeStamp >> 8);
        header[7] = (byte)(TimeStamp & 0xFF);
        header[8] = (byte)(Ssrc >> 24);
        header[9] = (byte)(Ssrc >> 16);
        header[10] = (byte)(Ssrc >> 8);
        header[11] = (byte)(Ssrc & 0xFF);

        //fill the extension header
        header[12] = (byte) (FrameLength >> 24);
        header[13] = (byte) (FrameLength >> 16);
        header[14] = (byte) (FrameLength >> 8);
        header[15] = (byte) (FrameLength & 0xFF);

        header[16] = (byte) (header[16] | FrameId >> 24);
        header[17] = (byte) (FrameId >> 16);
        header[18] = (byte) (FrameId >> 8);
        header[19] = (byte) (FrameId & 0xFF);

        header[20] = (byte) (header[20] | PktEnd << (7-0));
        header[20] = (byte) (header[20] | PktId >> 24);
        header[21] = (byte) (PktId >> 16);
        header[22] = (byte) (PktId >> 8);
        header[23] = (byte) (PktId & 0xFF);

        //fill the payload bitstream:
        payload_size = data_length;
        payload = new byte[data_length];

        //fill payload array of byte from data (given in parameter of the constructor)
        payload = Arrays.copyOf(data, payload_size);
    }

    //--------------------------
    //Constructor of an RTPpacket object from the packet bistream 
    //--------------------------
    public RTPpacket(byte[] packet, int packet_size)
    {
        //fill default fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE) 
        {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            for (int i=0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            //get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            for (int i=HEADER_SIZE; i < packet_size; i++)
                payload[i-HEADER_SIZE] = packet[i];

            //interpret the changing fields of the header:
            Version = (header[0] & 0xFF) >>> 6;
            PayloadType = header[1] & 0x7F;
            SequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            TimeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);

            //interpret the extension header fields
            FrameLength = unsigned_int(header[15]) + 256*unsigned_int(header[14]) + 65536*unsigned_int(header[13]) + 16777216*unsigned_int(header[12]);
            FrameId =  unsigned_int(header[19]) + 256*unsigned_int(header[18]) + 65536*unsigned_int(header[17]) + 16777216*unsigned_int(header[16] & 127);
            PktEnd = header[20] >> 7 & 1;
            PktId =  unsigned_int(header[23]) + 256*unsigned_int(header[22]) + 65536*unsigned_int(header[21]) + 16777216*unsigned_int(header[20] & 127);
        }
    }

    //--------------------------
    //getpayload: return the payload bistream of the RTPpacket and its size
    //--------------------------
    public int getpayload(byte[] data) {

        for (int i=0; i < payload_size; i++)
            data[i] = payload[i];

        return(payload_size);
    }

    //--------------------------
    //getpayload_length: return the length of the payload
    //--------------------------
    public int getpayload_length() {
        return(payload_size);
    }

    //--------------------------
    //getlength: return the total length of the RTP packet
    //--------------------------
    public int getlength() {
        return(payload_size + HEADER_SIZE);
    }

    //--------------------------
    //getpacket: returns the packet bitstream and its length
    //--------------------------
    public int getpacket(byte[] packet)
    {
        //construct the packet = header + payload
        for (int i=0; i < HEADER_SIZE; i++)
            packet[i] = header[i];
        for (int i=0; i < payload_size; i++)
            packet[i+HEADER_SIZE] = payload[i];

        //return total size of the packet
        return(payload_size + HEADER_SIZE);
    }

    //--------------------------
    //gettile_length: return the total length of current tile
    //--------------------------
    public int getframelength() {
        return FrameLength;
    }

    //--------------------------
    //gettile_id: return the id of current tile
    //--------------------------
    public int getframeid() {
        return FrameId;
    }

    //--------------------------
    //getpkt_id: return the packet id of current tile
    //--------------------------
    public int getpktid() {
        return PktId;
    }

    //--------------------------
    //getEndOfPkt: return the indicator of the end of packets
    //--------------------------
    public int getendofpkt() {
        return PktEnd;
    }

    //--------------------------
    //gettimestamp
    //--------------------------

    public int gettimestamp() {
        return(TimeStamp);
    }

    //--------------------------
    //getsequencenumber
    //--------------------------
    public int getsequencenumber() {
        return(SequenceNumber);
    }

    //--------------------------
    //getpayloadtype
    //--------------------------
    public int getpayloadtype() {
        return(PayloadType);
    }

    //--------------------------
    //print headers without the SSRC
    //--------------------------
    public void printheader()
    {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + Version
                           + ", Padding: " + Padding
                           + ", Extension: " + Extension
                           + ", CC: " + CC
                           + ", Marker: " + Marker
                           + ", PayloadType: " + PayloadType
                           + ", SequenceNumber: " + SequenceNumber
                           + ", TimeStamp: " + TimeStamp);

    }

    //return the unsigned value of 8-bit integer nb
    static int unsigned_int(int nb) {
        if (nb >= 0)
            return(nb);
        else
            return(256+nb);
    }
}