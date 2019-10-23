package com.example.shakil.kotlinimageuploadtofirebase

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val CAMERA_REQUEST = 1000
    private val PERMISSION_PICK_IMAGE = 1001
    internal var filePath: Uri? = null

    lateinit var dialog: AlertDialog

    //Firebase
    lateinit var storage: FirebaseStorage
    lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storage = FirebaseStorage.getInstance();
        storageReference = storage.reference

        dialog = SpotsDialog.Builder().setCancelable(false).setContext(this).build()

        btnCamera.setOnClickListener({ v ->
            openCamera()
        })

        btnChoose.setOnClickListener({ v ->
            chooseImage()
        })

        btnUpload.setOnClickListener({ v ->
            uploadImage()
        })
    }

    private fun uploadImage() {
        if (filePath != null){
            dialog.show()
            val reference = storageReference.child("images/" + UUID.randomUUID().toString())
            reference.putFile(filePath!!).addOnSuccessListener { taskSnapshot ->
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "Uploaded", Toast.LENGTH_SHORT)
                    .show()
            }.addOnFailureListener{e ->
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT)
                    .show()
            }.addOnProgressListener { taskSnapshot ->
                val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                dialog.setMessage("Uploaded $progress %")
            }
        }
    }

    private fun chooseImage() {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {

                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "image/*"
                    startActivityForResult(
                        Intent.createChooser(intent, "Select Image"),
                        PERMISSION_PICK_IMAGE
                    )
                } else {
                    Toast.makeText(this@MainActivity, "Permission Denied !", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                token!!.continuePermissionRequest()
            }
        }).check()
    }

    private fun openCamera() {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val values = ContentValues()
                    values.put(MediaStore.Images.Media.TITLE, "New Picture")
                    values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                    filePath =
                        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, filePath)
                    startActivityForResult(cameraIntent, CAMERA_REQUEST)
                } else {
                    Toast.makeText(this@MainActivity, "Permission Denied !", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                token!!.continuePermissionRequest()
            }
        }).check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PERMISSION_PICK_IMAGE) {
                if (data != null) {
                    if (data.data != null) {
                        filePath = data.data
                        try {
                            val bitmap =
                                MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                            imageView.setImageBitmap(bitmap)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            if (requestCode == CAMERA_REQUEST) {

                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                    imageView.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
