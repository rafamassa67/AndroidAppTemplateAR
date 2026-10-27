package com.ifpr.androidapptemplatear.ui

import androidx.fragment.app.Fragment
import com.ifpr.androidapptemplatear.R
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import com.ifpr.androidapptemplatear.databinding.FragmentCameraBinding
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import okio.IOException
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var modelNode: ArModelNode
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var babyButton: ExtendedFloatingActionButton
    private lateinit var modelNameText: TextView
    private lateinit var hintText: TextView

    private lateinit var sharedViewModel: SharedModelViewModel

    private val CAMERA_PERMISSION_CODE = 1001

    private var currentModelFile: String = "neutrofilo.glb"

    // Som tocado toda vez que um modelo é colocado (res/raw/dark_triad.mp3)
    private var darkTriadPlayer: MediaPlayer? = null

    private var _binding: FragmentCameraBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sharedViewModel = ViewModelProvider(requireActivity())[SharedModelViewModel::class.java]
        sharedViewModel.selectedModel.observe(viewLifecycleOwner) { path ->
            loadModel(path)
        }

        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        modelNameText = view.findViewById(R.id.modelName)
        hintText = view.findViewById(R.id.hintText)
        modelNameText.text = currentModelFile.toModelTitle()

        placeButton = view.findViewById(R.id.place)
        placeButton.setOnClickListener {
            placeModel()
        }

        babyButton = view.findViewById(R.id.babyButton)
        babyButton.setOnClickListener {
            switchToBaby()
        }

        // Com edge-to-edge a barra superior precisa descer a altura da status bar
        val topBar = view.findViewById<View>(R.id.topBar)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + dp(12)
            }
            insets
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkCameraPermission()
    }

    private fun placeModel() {
        if (!::modelNode.isInitialized) return

        modelNode.anchor()
        modelNode.isEditable = true
        modelNode.isScaleEditable = false

        sceneView.planeRenderer.isVisible = false
        hintText.setText(R.string.hint_placed)

        playDarkTriad()
    }

    /**
     * Toca o dark_triad toda vez que um modelo é colocado.
     * Se já estiver tocando, reinicia do começo.
     */
    private fun playDarkTriad() {
        releasePlayer()
        darkTriadPlayer = MediaPlayer.create(requireContext(), R.raw.dark_triad)?.apply {
            setOnCompletionListener { mp ->
                mp.release()
                if (darkTriadPlayer === mp) darkTriadPlayer = null
            }
            start()
        }
    }

    private fun releasePlayer() {
        darkTriadPlayer?.let {
            try {
                it.release()
            } catch (_: Exception) {
            }
        }
        darkTriadPlayer = null
    }

    /**
     * TESTE: independente do modelo atual, vira a morsa bebê.
     * Depois dá pra trocar por um mapa "modelo -> versão bebê" (ver BABY_MODEL).
     */
    private fun switchToBaby() {
        if (!::modelNode.isInitialized) {
            Toast.makeText(requireContext(), "Aguarde a câmera iniciar", Toast.LENGTH_SHORT).show()
            return
        }
        loadModel(BABY_MODEL)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            initAR()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAR()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera negada", Toast.LENGTH_LONG).show()
        }
    }

    private fun initAR() {
        loadModel(currentModelFile)
    }

    private fun loadModel(fileName: String) {
        try {
            sceneView.removeChild(modelNode)
        } catch (e: Exception) {

        }

        currentModelFile = fileName
        modelNameText.text = fileName.toModelTitle()
        hintText.setText(R.string.hint_find_surface)

        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = getFilePath(requireContext(),fileName),
                centerOrigin = Position(0f,0f,1f),

            ) {
                sceneView.planeRenderer.isVisible = true
            }
            // Permitir interação com o modelo
            isEditable = true
            isScaleEditable = false

        }

        sceneView.addChild(modelNode)
    }

    private fun getFilePath(context: Context?, fileName: String): String {
        try {
            context?.assets?.open(fileName)?.close() // Tenta abrir o arquivo dos assets
            return fileName // Se abrir sem erro, presume-se que está nos assets
        } catch (e: IOException) {
            // Arquivo não encontrado nos assets, agora vamos procurar no filesDir
            val internalFile = File(context?.filesDir, fileName)
            if (internalFile.exists()) {
                return  Uri.fromFile(internalFile).toString()
            } else {
                return "Arquivo não encontrado em assets nem em filesDir: $fileName"
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        /** Modelo usado pelo botão "Bebê" (por enquanto o mesmo para qualquer modelo). */
        private const val BABY_MODEL = "morsa_bebe.glb"
    }
}
