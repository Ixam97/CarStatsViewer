package com.ixam97.carStatsViewer.mailSender

import android.graphics.Bitmap
import com.ixam97.carStatsViewer.CarStatsViewer
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.activation.FileDataSource
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.io.*
import java.security.Security
import java.util.*

class MailSender(private val user: String, private val password: String, private val server: String) : Authenticator() {
    private val session: Session
    private val _multipart: Multipart = MimeMultipart()

    init {
        val props = Properties()
        props.setProperty("mail.transport.protocol", "smtp")
        props.setProperty("mail.host", server)
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "465"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.socketFactory.port"] = "465"
        props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        props["mail.smtp.socketFactory.fallback"] = "false"
        props.setProperty("mail.smtp.quitwait", "false")
        session = Session.getDefaultInstance(props, this)
    }

    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(user, password)
    }

    @Synchronized
    fun sendMail(subject: String?, body: String, sender: String?, recipients: String) {
        val message = MimeMessage(session)
        val handler = DataHandler(ByteArrayDataSource(body.toByteArray(), "text/plain"))
        message.sender = InternetAddress(sender)
        message.subject = subject
        message.dataHandler = handler
        val messageBodyPart: BodyPart = MimeBodyPart()
        messageBodyPart.setText(body)
        _multipart.addBodyPart(messageBodyPart)
        message.setContent(_multipart)
        if (recipients.indexOf(',') > 0) message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(recipients)
        ) else message.setRecipient(
            Message.RecipientType.TO, InternetAddress(recipients)
        )
        Transport.send(message)
    }

    fun addAttachment(file: File) {
        val messageBodyPart: BodyPart = MimeBodyPart()
        val source: DataSource = FileDataSource(file)
        messageBodyPart.dataHandler = DataHandler(source)
        messageBodyPart.fileName = file.name
        _multipart.addBodyPart(messageBodyPart)
    }

    fun addAttachment(content: String, fileName: String) {
        val messageBodyPart: BodyPart = MimeBodyPart()
        messageBodyPart.dataHandler = DataHandler(content, "text/plain")
        messageBodyPart.fileName = fileName
        _multipart.addBodyPart(messageBodyPart)
    }

    fun addAttachment(content: Bitmap, fileName: String) {
        val messageBodyPart = MimeBodyPart()

        val dir = CarStatsViewer.appContext.cacheDir.toString()
        val file = File(dir, "$fileName.jpeg")
        val outputStream = BufferedOutputStream(FileOutputStream(file))

        content.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        outputStream.close()

        messageBodyPart.attachFile(file)
        _multipart.addBodyPart(messageBodyPart)
    }

    inner class ByteArrayDataSource : DataSource {
        private var data: ByteArray
        private var type: String? = null

        constructor(data: ByteArray, type: String?) : super() {
            this.data = data
            this.type = type
        }

        constructor(data: ByteArray) : super() {
            this.data = data
        }

        fun setType(type: String?) {
            this.type = type
        }

        override fun getContentType(): String {
            return this.type?: "application/octet-stream"
        }

        @Throws(IOException::class)
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(data)
        }

        override fun getName(): String {
            return "ByteArrayDataSource"
        }

        @Throws(IOException::class)
        override fun getOutputStream(): OutputStream {
            throw IOException("Not Supported")
        }
    }

    companion object {
        init {
            Security.addProvider(JSSEProvider())
        }
    }
}