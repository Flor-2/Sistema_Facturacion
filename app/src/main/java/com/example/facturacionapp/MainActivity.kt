package com.example.facturacionapp


import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.text.TextPaint
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Canvas
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.facturacionapp.databinding.ActivityMainBinding
import com.example.facturacionapp.db.Cliente
import com.example.facturacionapp.db.Factura
import com.example.facturacionapp.db.FacturacionDataBase
import com.example.facturacionapp.db.Producto
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var database: FacturacionDataBase
    }

    private lateinit var editRnc: EditText
    private lateinit var editNombreCliente: EditText
    private lateinit var editProductoId: EditText
    private lateinit var editCantidad: EditText
    private lateinit var btnProcesar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editRnc = findViewById(R.id.editRnc)
        editNombreCliente = findViewById(R.id.editNombreCliente)
        editProductoId = findViewById(R.id.editProductoId)
        editCantidad = findViewById(R.id.editCantidad)
        btnProcesar = findViewById(R.id.btnProcesar)

        // Insertar un producto de prueba inicial en segundo plano
        insertarProductoDemo()

        btnProcesar.setOnClickListener {
            procesarFactura()
        }
    }

    private fun procesarFactura() {

        val rnc = editRnc.text.toString()
        val nombreCliente = editNombreCliente.text.toString()
        val prodIdStr = editProductoId.text.toString()
        val cantStr = editCantidad.text.toString()

        if (rnc.isEmpty() || nombreCliente.isEmpty() || prodIdStr.isEmpty() || cantStr.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val prodId = prodIdStr.toInt()
        val cantidad = cantStr.toInt()
        val rncid = rnc.toInt()

        // Ejecutar consulta SQLite en hilo secundario para no bloquear UI
        Thread {
            val dao = FacturacionDataBase.getDataBase(this).facturacionDao()
            val producto = dao.getProductoById(prodId)


            if (producto == null) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Producto no encontrado", Toast.LENGTH_SHORT)
                        .show()
                }
                return@Thread
            }

            if (producto.stock < cantidad) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Stock insuficiente(${producto.stock} disponibles)",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@Thread
            }


            // Registrar/Actualizar cliente
            val cliente = Cliente(rncid, nombreCliente, "Dirección genérica")
            dao.insertCliente(cliente)

            // Calcular Total y Actualizar Stock
            val total = producto.precio * cantidad
            producto.stock -= cantidad

            dao.updateProducto(producto)

            //Registrar Factura
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            val factura = Factura(
                rncCedula = rncid,
                idProducto = prodId,
                cantidad = cantidad,
                Total = total,
                fecha = fecha
            )

            dao.insertFactura(factura)

            // Volver al hilo de la UI para mostrar resultado
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "¡Factura Creada! Total: $$total",
                    Toast.LENGTH_SHORT
                ).show()

                GenerarPdf(factura,producto,cliente)
                limpiarCampos()
            }
        }.start()


    }

    private fun insertarProductoDemo() {
        Thread {
            val dao = FacturacionDataBase.getDataBase(this).facturacionDao()
            if (dao.getProductoById(1) == null) {
                dao.insertProducto(
                    Producto(
                        idProducto = 1,
                        nombre = "Laptop",
                        precio = 500.0,
                        stock = 10
                    )
                )
            }
            
            /*else{
                val p : Producto? = dao.getProductoById(1)

                if(p!=null){
                    p.stock = 20
                    dao.updateProducto(p)
                }
            }*/



        }.start()
    }

    private fun limpiarCampos() {
        editRnc.text.clear()
        editNombreCliente.text.clear()
        editProductoId.text.clear()
        editCantidad.text.clear()
    }

    private fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun GenerarPdf(factura: Factura, producto: Producto, cliente: Cliente) {

        val docFolder =
            File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString())
        if(!docFolder.exists()){
            docFolder.mkdir()
        }

        val pdfDocument = PdfDocument()


        // Tamaño aproximado de una página A4
        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            1
        ).create()

        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas

        // Paint principal
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Paint para títulos
        val paintTitulo = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 22f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        // Paint para encabezados
        val paintHeader = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        // Paint para líneas
        val paintLinea = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // ------------------------------------------------
        // TÍTULO
        // ------------------------------------------------

        canvas.drawText( "FACTURA", 250f, 50f, paintTitulo)

        // ------------------------------------------------
        // FECHA
        // ------------------------------------------------

        val fecha = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())

        canvas.drawText(
            "Fecha: $fecha",
            420f,
            80f,
            paint
        )

        // ------------------------------------------------
        // DATOS DEL CLIENTE
        // ------------------------------------------------

        val clienteTop = 100f
        val clienteBottom = 180f

        // Rectángulo de cliente
        canvas.drawRect(
            40f,
            clienteTop,
            555f,
            clienteBottom,
            paintLinea
        )

        canvas.drawText(
            "DATOS DEL CLIENTE",
            50f,
            120f,
            paintHeader
        )

        canvas.drawText(
            "RNC: ${cliente.rncCedula}",
            50f,
            145f,
            paint
        )

        canvas.drawText(
            "Nombre: ${cliente.nombre}",
            250f,
            145f,
            paint
        )

        canvas.drawText(
            "Dirección: ${cliente.direccion}",
            50f,
            170f,
            paint
        )

        // ------------------------------------------------
        // TABLA DE PRODUCTOS
        // ------------------------------------------------

        val tablaTop = 210f
        val filaAlto = 35f

        // Posiciones de las columnas

        val xId = 40f
        val xNombre = 90f
        val xPrecio = 300f
        val xCantidad = 390f
        val xSubtotal = 470f

        val tablaDerecha = 555f

        // Encabezado
        canvas.drawRect(
            xId,
            tablaTop,
            tablaDerecha,
            tablaTop + filaAlto,
            paintLinea
        )

        // Líneas verticales

        canvas.drawLine(
            xNombre,
            tablaTop,
            xNombre,
            tablaTop + filaAlto,
            paintLinea
        )

        canvas.drawLine(
            xPrecio,
            tablaTop,
            xPrecio,
            tablaTop + filaAlto,
            paintLinea
        )

        canvas.drawLine(
            xCantidad,
            tablaTop,
            xCantidad,
            tablaTop + filaAlto,
            paintLinea
        )

        canvas.drawLine(
            xSubtotal,
            tablaTop,
            xSubtotal,
            tablaTop + filaAlto,
            paintLinea
        )

        // Texto del encabezado

        canvas.drawText(
            "ID",
            55f,
            tablaTop + 23f,
            paintHeader
        )

        canvas.drawText(
            "Nombre",
            100f,
            tablaTop + 23f,
            paintHeader
        )

        canvas.drawText(
            "Precio",
            310f,
            tablaTop + 23f,
            paintHeader
        )

        canvas.drawText(
            "Cantidad",
            397f,
            tablaTop + 23f,
            paintHeader
        )

        canvas.drawText(
            "Subtotal",
            480f,
            tablaTop + 23f,
            paintHeader
        )

        // ------------------------------------------------
        // PRODUCTOS
        // ------------------------------------------------

        var y = tablaTop + filaAlto

        var total = 0.0

            val subtotal =
                producto.precio * factura.cantidad

            total += subtotal

            // Dibujar fila

            canvas.drawRect(
                xId,
                y,
                tablaDerecha,
                y + filaAlto,
                paintLinea
            )

            // Líneas verticales

            canvas.drawLine(
                xNombre,
                y,
                xNombre,
                y + filaAlto,
                paintLinea
            )

            canvas.drawLine(
                xPrecio,
                y,
                xPrecio,
                y + filaAlto,
                paintLinea
            )

            canvas.drawLine(
                xCantidad,
                y,
                xCantidad,
                y + filaAlto,
                paintLinea
            )

            canvas.drawLine(
                xSubtotal,
                y,
                xSubtotal,
                y + filaAlto,
                paintLinea
            )

            // Datos

            canvas.drawText(
                producto.idProducto.toString(),
                55f,
                y + 23f,
                paint
            )

            canvas.drawText(
                producto.nombre,
                100f,
                y + 23f,
                paint
            )

            canvas.drawText(
                String.format(
                    Locale.US,
                    "%.2f",
                    producto.precio
                ),
                310f,
                y + 23f,
                paint
            )

            canvas.drawText(
                factura.cantidad.toString(),
                420f,
                y + 23f,
                paint
            )

            canvas.drawText(
                String.format(
                    Locale.US,
                    "%.2f",
                    subtotal
                ),
                480f,
                y + 23f,
                paint
            )

            y += filaAlto


        // ------------------------------------------------
        // TOTAL
        // ------------------------------------------------

        y += 20f

        canvas.drawText(
            "TOTAL:",
            400f,
            y,
            paintHeader
        )

        canvas.drawText(
            String.format(
                Locale.US,
                "$ %.2f",
                total
            ),
            470f,
            y,
            paintHeader
        )

        // Finalizar página
        pdfDocument.finishPage(page)

        // Guardar PDF
        //FileOutputStream(archivo).use { outputStream -> pdfDocument.writeTo(outputStream) }

        val file = File(docFolder.absoluteFile,"archivo.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this,"PDF CREADO", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        // Cerrar documento
        pdfDocument.close()

    }
}