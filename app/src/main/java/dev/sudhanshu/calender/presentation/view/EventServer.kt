package dev.sudhanshu.calender.presentation.view

import fi.iki.elonen.NanoHTTPD
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class EventServer(port: Int) : NanoHTTPD(port){
    init{
        makeSecure()
    }
    override fun serve(session: IHTTPSession): Response{

        if(session.method == Method.POST){
            val data = mutableMapOf<String, String>()
            try{
                // get the request body
                session.parseBody(data)
                val requestBody = data["data"]?:"No data received"
                return newFixedLengthResponse("Received POST data: $requestBody")
            }
            catch(e: Exception){
                e.printStackTrace()
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "Error processing POST request")
            }
        }
        else{
            return newFixedLengthResponse("This is a simple Http Server!")
        }


    }

    private fun makeSecure(){
        // http -> https
        try{
            val keystoreFile  = FileInputStream("./secret/keystore.jks")
            val keystorePassword = "123456".toCharArray()
            // Load the keystore
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            keystore.load(keystoreFile, keystorePassword)

            // Set up key manager factory
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keystore, keystorePassword)

            // Create SSL context
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            // Set up NanoHTTPD to use SSL
            makeSecure(sslContext.serverSocketFactory, null)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

}