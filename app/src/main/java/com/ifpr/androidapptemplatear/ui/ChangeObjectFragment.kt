package com.ifpr.androidapptemplatear.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ifpr.androidapptemplatear.R

class ChangeObjectFragment : Fragment() {

    private lateinit var sharedViewModel: SharedModelViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_changeobject,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel =
            ViewModelProvider(requireActivity())[SharedModelViewModel::class.java]

        // Com edge-to-edge o cabeçalho precisa descer a altura da status bar
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top)
            insets
        }

        carregarModelos()
    }

    /**
     * Procura automaticamente todos os arquivos .glb
     * existentes na pasta assets.
     */
    private fun carregarModelos() {

        val container =
            view?.findViewById<LinearLayout>(R.id.categoryContainer)

        container?.removeAllViews()

        val modelos = requireContext()
            .assets
            .list("")
            ?.filter { arquivo ->
                arquivo.endsWith(".glb", ignoreCase = true)
            }
            ?.sorted()
            ?: emptyList()

        if (modelos.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Nenhum modelo 3D encontrado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        view?.findViewById<TextView>(R.id.subtitle)?.text =
            getString(R.string.models_subtitle_format, modelos.size)

        modelos.forEach { arquivo ->
            adicionarModelo(container, arquivo)
        }
    }

    /**
     * Cria um card para cada modelo encontrado.
     * O card inteiro é clicável.
     */
    private fun adicionarModelo(
        container: LinearLayout?,
        arquivo: String
    ) {
        val card = layoutInflater.inflate(R.layout.item_model, container, false)

        val nomeModelo = arquivo.toModelTitle()

        card.findViewById<TextView>(R.id.modelTitle).text = nomeModelo
        card.findViewById<TextView>(R.id.modelAvatar).text =
            nomeModelo.first().uppercase()

        card.setOnClickListener {
            /*
             * Informa para a tela da câmera
             * qual modelo deve ser carregado.
             */
            sharedViewModel.selectModel(arquivo)

            /*
             * Abre a tela de Realidade Aumentada.
             */
            findNavController().navigate(
                R.id.navigation_camera
            )
        }

        container?.addView(card)
    }
}
