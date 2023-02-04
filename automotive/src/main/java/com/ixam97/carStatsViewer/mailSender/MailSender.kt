package com.ixam97.carStatsViewer.mailSender

import javax.mail.internet.MimeMultipart
import kotlin.jvm.Synchronized
import kotlin.Throws
import javax.mail.internet.MimeMessage
import javax.activation.DataHandler
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.activation.FileDataSource
import com.ixam97.carStatsViewer.mailSender.JSSEProvider
import java.io.*
import java.lang.Exception
import java.security.Security
import java.util.*
import javax.activation.DataSource
import javax.mail.*

class MailSender(private val user: String, private val password: String) : Authenticator() {
    private val mailhost = "smtp.strato.de"
    private val session: Session
    private val _multipart: Multipart = MimeMultipart()

    init {
        val props = Properties()
        props.setProperty("mail.transport.protocol", "smtp")
        props.setProperty("mail.host", mailhost)
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.port"] = "465"
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