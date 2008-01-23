/*
 * Copyright 2004-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#ifdef _MSC_VER
#include <winsock.h>
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#endif


/**
 * Prints a usage message.
 */
void IOServer_usage() {
    fprintf(stderr, "Usage: ChannelIO [-options]\n");
    fprintf(stderr, "where options include:\n");
    fprintf(stderr, "\n");
    fprintf(stderr, "    -port:<port>   the port to listen on (default=9090)\n");
    fprintf(stderr, "    -d             run in debug mode\n");
    fprintf(stderr, "    -t<n>          show timing info every 'n' IO operations\n");
    jvmUsage();
    fprintf(stderr, "\n");
}

/**
 * Fills a buffer from a socket.
 *
 * @param sd         the descriptor of the socket to read from
 * @param buffer     the buffer to read into
 * @param size       the size of the buffer
 */
void IOServer_receiveData(const int sd, char *buffer, const int size) {
    int received = 0;

    while (received != size) {
        char *ptr = buffer + received;
        int remaining = size - received;
        int got = recv(sd, ptr, remaining, 0);
        if (got == -1) {
            fatalVMError("error receiving data");
        }
        received += got;
    }
}

/**
 * Sends a buffer of data to a socket.
 *
 * @param sd         the descriptor of the socket to write to
 * @param buffer     the buffer to send
 * @param size       the size of the buffer
 */
void IOServer_sendData(const int sd, char *buffer, const int size) {
    int sent = 0;

    while (sent != size) {
        char *ptr = buffer + sent;
        int remaining = size - sent;
        int put = send(sd, ptr, remaining, 0);
        if (put == -1) {
            fatalVMError("error sending data");
        }

        sent += put;
    }
}

/**
 * Read an int from a socket.
 *
 * @param sd     the socket to read from stream
 * @param name   debugging label
 * @return the int value
 */
static int IOServer_readInt(int sd, const char *name, boolean debug) {
    int value;
    int got = recv(sd, (char *)&value, 4, 0);
    if (got != 4) {
        fprintf(stderr, "error while reading '%s' from socket (got=%d)\n", name, got);
        fatalVMError("socket read error");
    }

    // convert from network order
    value = ntohl(value);

    if (debug) fprintf(stderr, "%s=%d ", name, value);
    return value;
}

/**
 * Write an int to a socket
 *
 * @param sd     the socket to write to
 * @param name   debugging label
 * @param value  the value to write
 */
static void IOServer_writeInt(int sd, const char *name, int value, boolean debug) {
    int put;

    // convert to network order
    value = htonl(value);

    put = send(sd, (char *)&value, 4, 0);
    if (put != 4) {
        fprintf(stderr, "error while sending '%s' to socket (put=%d)\n", name, put);
        fatalVMError("socket write error");
    }
    if (debug) fprintf(stderr, "%s=%d ", name, value);
}

/**
 * Reads a byte array from the I/O port and returns the corresponding JVM byte[] object.
 *
 * @param sd       the socket to read from
 * @param length   the length of the byte array
 * @param isString specifies if the byte array is actually a STRING_OF_BYTES
 * @return the JVM object created from the array data
 */
static Address readIOPByteArray(int sd, int length, boolean isString) {
    unsigned char *array = (unsigned char *)newBuffer(length, "readIOPByteArray", true);
    Address jvmObject;

    // read data
    IOServer_receiveData(sd, (char *)array, length);
    jvmObject = isString ? createJVMString(array, CID_STRING_OF_BYTES, length) : createJVMByteArray(array, length, true);
    freeBuffer(array);
    return jvmObject;
}

/**
 * Reads a char array from the I/O port and returns the corresponding JVM char[] object.
 *
 * @param sd        the socket to read from
 * @param length    the length of the char array
 * @param isString  specifies if the char array is actually a STRING
 * @return the JVM object created from the array data
 */
static Address readIOPCharArray(int sd, int length, boolean isString) {
    int size = length * 2;
    unsigned short* array = (unsigned short *)newBuffer(size, "readIOPCharArray", true);
    Address jvmObject;
    int i;

    // read data
    IOServer_receiveData(sd, (char *)array, size);

    // convert from network order
    for (i = 0; i != length; ++i) {
        array[i] = ntohs(array[i]);
    }

    jvmObject = isString ? createJVMString(array, CID_STRING, length) : createJVMCharArray(array, length, true);
    freeBuffer(array);
    return jvmObject;
}

/**
 * Reads a char array from the I/O port and returns the corresponding JVM char[] object.
 *
 * @param sd     the socket to read from
 * @param length the length of the char array
 * @return the JVM object created from the array data
 */
static Address readIOPIntArray(int sd, int length) {
    int size = length * 4;
    unsigned int* array = (unsigned int *)newBuffer(size, "readIOPIntArray", true);
    Address jvmIntArray;
    int i;

    // read data
    IOServer_receiveData(sd, (char *)array, size);

    // convert from network order
    for (i = 0; i != length; ++i) {
        array[i] = ntohl(array[i]);
    }

    jvmIntArray = createJVMIntArray(array, length, true);
    freeBuffer(array);
    return jvmIntArray;
}

/**
 * Read an object from the socket
 *
 * @param sd   the socket to read from
 * @return the corresponding JVM object
 */
static Address IOServer_readObject(int sd, boolean debug) {
    int classID = IOServer_readInt(sd, "object:classID", debug);
    int length = IOServer_readInt(sd, "object:length", debug);
    Address jvmObject;

    if (classID == 0) {
        return null;
    } else if (classID == CID_BYTE_ARRAY || classID == CID_STRING_OF_BYTES) {
        jvmObject = readIOPByteArray(sd, length, classID == CID_STRING_OF_BYTES);
    } else if (classID == CID_CHAR_ARRAY || classID == CID_STRING) {
        jvmObject = readIOPCharArray(sd, length, classID == CID_STRING);
    } else if (classID == CID_INT_ARRAY) {
        jvmObject = readIOPIntArray(sd, length);
    } else {
        fprintf(stderr, "Invalid class ID: %d\n", classID);
        fatalVMError("bad class ID");
    }
    return lockJVMObject(jvmObject);
}


void setReceiveBufferSize(int sd, int size) {
    setsockopt(sd, SOL_SOCKET, SO_RCVBUF, (char *)&size, sizeof(size));
}

int getReceiveBufferSize(int sd) {
    int size, sizeLength = 4;
    getsockopt(sd, SOL_SOCKET, SO_RCVBUF, (char *)&size, &sizeLength);
    return size;
}

typedef struct IOTimeInfoStruct {
    jlong execute;
    jlong receive;
    jlong send;
    int count;
} IOTimeInfo;


/**
 * This function implements a server that waits for a connection from a client
 * and then reads a prescribed chunk of data from the client.
 */
int IOServer_main (int argc, char *argv[]) {

    int serverSocket;    // socket that listens for a connection
    struct sockaddr_in serverAddress;
    int port = 9090;
    boolean debug = false;
    int timing = 0;
    IOTimeInfo timingInfo;
    char *VMArgs[MAX_JVM_ARGS];
    int VMArgsCount;

    // Parse the command line arguments
    while (argc-- > 0) {
        char *arg = *argv;
        if (startsWith(arg, "-port:")) {
            port = atoi(arg + 6);
        } else if (equals(arg, "-d")) {
            debug = true;
        } else if (startsWith(arg, "-t")) {
            timing = atoi(arg+2);
        } else if (startsWith(arg, "-J")) {
            if (VMArgsCount >= MAX_JVM_ARGS) {
                fatalVMError("too many '-J' flags");
            }
            VMArgs[VMArgsCount++] = arg + 2;
        } else {
            fprintf(stderr, "Unknown option: %s\n", arg);
            IOServer_usage();
            fatalVMError(null);
        }
        argv++;
    }

    // Initialize the embedded JVM
    CIO_initialize(null, "squawk.jar", VMArgs, VMArgsCount);

    if (timing != 0) {
        memset(&timingInfo, 0, sizeof(timingInfo));
    }


#ifdef _MSC_VER
    {
        WSADATA wsaData;
        int result = WSAStartup(0X0101, &wsaData);
        if (result != 0) {
            fprintf(stderr, "WSAStartup(...) returned \n", result);
            fatalVMError(null);
        }
    }
#endif

    fprintf(stderr, "Starting server on port %d\n", port);

    /*
     * Create the socket for accepting connections.
     */
    serverSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSocket < 0) {
        fprintf(stderr, "cannot create socket\n");
        fatalVMError(null);
    }

    /*
     * Open socket and bind it to the server port.
     */
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = htonl(INADDR_ANY);
    serverAddress.sin_port = htons(port);

//setReceiveBufferSize(serverSocket, 8192);
    if (bind(serverSocket, (struct sockaddr *) &serverAddress, sizeof(serverAddress)) < 0) {
        fprintf(stderr, "cannot bind to port %d\n", port);
        fatalVMError(null);
    }

fprintf(stderr, "server socket receive buffer size = %d\n", getReceiveBufferSize(serverSocket));

    for (;;) {
        struct sockaddr_in clientAddress;
        int size;
        int sd;

        /*
         * Block until a connection attempt from a client is made.
         */
        if (debug) fprintf(stderr, "listening on port %d\n", port);
        listen(serverSocket, 5);

        size = sizeof(clientAddress);
        sd = accept(serverSocket, (struct sockaddr *)&clientAddress, &size);
        if (sd < 0) {
            fprintf(stderr, "cannot accept connection\n");
            fatalVMError(null);
        }

//setReceiveBufferSize(sd, 8192);
fprintf(stderr, "accept socket receive buffer size = %d\n", getReceiveBufferSize(sd));


        if (debug) fprintf(stderr, "Got connection\n");
        for (;;) {
            jlong start = (timing == 0 ? 0 : sysTimeMicros());
            jlong end = 0;
            Address r1 = null;
            int status = -1;
            int low, high;
            int ignore = (debug ? fprintf(stderr, "IO server receiving: ") : 0);
            int context= IOServer_readInt(sd, "context", debug);        // 0
            int op     = IOServer_readInt(sd, "op", debug);             // 1
            int channel= IOServer_readInt(sd, "channel", debug);        // 2
            int i1     = IOServer_readInt(sd, "i1", debug);             // 3
            int i2     = IOServer_readInt(sd, "i2", debug);             // 4
            int i3     = IOServer_readInt(sd, "i3", debug);             // 5
            int i4     = IOServer_readInt(sd, "i4", debug);             // 6
            int i5     = IOServer_readInt(sd, "i5", debug);             // 7
            int i6     = IOServer_readInt(sd, "i6", debug);             // 8
            int returnBufLength = IOServer_readInt(sd, "length", debug);         // 9
            Address s1  = IOServer_readObject(sd, debug);
            Address s2  = IOServer_readObject(sd, debug);

            if (timing != 0) {
                end = sysTimeMicros();
                timingInfo.receive += (end - start);
                start = end;
            }

            if (debug) fprintf(stderr, "\n");
            if (returnBufLength != 0) {
                r1 = (*JNI_env)->NewByteArray(JNI_env, returnBufLength);
            }

            status = (*JNI_env)->CallStaticIntMethod(JNI_env, ChannelIO.clazz, ChannelIO.execute, context, op, channel, i1, i2, i3, i4, i5, i6, s1, s2, r1);
            jni_check("CIO_execute failure");

            if (s1 != null) freeJVMObject(s1);
            if (s2 != null) freeJVMObject(s2);

            low = (*JNI_env)->CallStaticIntMethod(JNI_env, ChannelIO.clazz, ChannelIO.execute, context, ChannelConstants_CONTEXT_GETRESULT, -1, 0, 0, 0, 0, 0, 0, null, null, null);
            high = (*JNI_env)->CallStaticIntMethod(JNI_env, ChannelIO.clazz, ChannelIO.execute, context, ChannelConstants_CONTEXT_GETRESULT, -1, 0, 0, 0, 0, 0, 0, null, null, null);

            if (timing != 0) {
                end = sysTimeMicros();
                timingInfo.execute += (end - start);
                start = end;
            }

            if (debug) fprintf(stderr, "IO server sending: ");
            IOServer_writeInt(sd, "magic",  0xCAFEBABE, debug);
            IOServer_writeInt(sd, "status", status, debug);
            IOServer_writeInt(sd, "r-low ", low, debug);
            IOServer_writeInt(sd, "r-high", high, debug);
            IOServer_writeInt(sd, "resLth", returnBufLength, debug);
            if (r1 != null) {
                jboolean isCopy;
                signed char *array = (*JNI_env)->GetByteArrayElements(JNI_env, (jbyteArray)r1, &isCopy);
                IOServer_sendData(sd, (char *)array, returnBufLength);
                (*JNI_env)->ReleaseByteArrayElements(JNI_env, r1, array, JNI_ABORT);
                freeJVMObject(r1);
            }
            if (debug) fprintf(stderr, "\n");

            if (timing != 0) {
                end = sysTimeMicros();
                timingInfo.send += (end - start);
                timingInfo.count++;

                if ((timingInfo.count % timing) == 0) {
                    jlong total = (timingInfo.receive + timingInfo.execute + timingInfo.send);
                    fprintf(stderr, "average time per IO operation:\n");
                    fprintf(stderr, format("    receive: %Lusec\n"), (timingInfo.receive / timingInfo.count));
                    fprintf(stderr, format("    send:    %Lusec\n"), (timingInfo.send / timingInfo.count));
                    fprintf(stderr, format("    execute: %Lusec\n"), (timingInfo.execute / timingInfo.count));
                    fprintf(stderr, format("    total:   %Lusec\n"), (total / timingInfo.count));
                }
            }
        }
    }
}

