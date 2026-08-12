package com.gerfrota.lite.core

import android.content.Context
import java.io.File

/**
 * Extensão para limpar nomes de arquivos e pastas, substituindo caracteres 
 * inválidos do sistema de arquivos (Windows/Android) por underline.
 */
fun String.sanitized(): String = this.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

object PathHelper {
    
    // --- DIRETÓRIOS BASE ---
    /** 
     * Diretório base do app. 
     * Prioriza o armazenamento externo privado (não requer permissão de storage no Android 10+).
     * Fallback para o diretório interno caso o dispositivo não tenha SD Card/Media montada.
     */
    fun base(ctx: Context): File = ctx.getExternalFilesDir(null) ?: ctx.filesDir
    
    // --- PASTAS GENÉRICAS ---
    fun pasta(ctx: Context, nome: String): File = File(base(ctx), nome.sanitized()).apply { mkdirs() }

    // --- PRONTUÁRIOS (Relatórios em TXT gerados pelo app) ---
    fun prontuarioPlaca(ctx: Context, placa: String) =
        File(pasta(ctx, "ProntuarioPlaca"), "${placa.sanitized()}_prontuario.txt")

    fun prontuarioAbastecimento(ctx: Context, placa: String) =
        File(pasta(ctx, "ProntuariosAbastecimento"), "Abastecimento_${placa.sanitized()}.txt")

    fun prontuarioArla(ctx: Context, placa: String) =
        File(pasta(ctx, "ProntuariosArla"), "ARLA_${placa.sanitized()}.txt")
        
    fun prontuarioViagem(ctx: Context, modelo: String, placa: String) =
        File(pasta(ctx, "ProntuarioViagens"), "Viagem_${modelo.sanitized()}_${placa.sanitized()}.txt")

    // --- ANEXOS E MÍDIAS (Fotos, PDFs, Assinaturas, Documentos) ---
    fun pastaFotosVeiculos(ctx: Context) = pasta(ctx, "FotosVeiculos")
    fun pastaCNH(ctx: Context) = pasta(ctx, "CNHdosMotoristas")
    fun pastaDocumentosVeiculos(ctx: Context) = pasta(ctx, "DocumentosVeiculos")
    fun pastaCardsManutencao(ctx: Context) = pasta(ctx, "CardsManutencao")
    fun pastaAssinaturas(ctx: Context) = pasta(ctx, "Assinaturas")
    fun pastaViagensFretes(ctx: Context) = pasta(ctx, "ViagensFretes")

    // --- NOTAS DE SERVIÇO / PDFs GERENCIAIS ---
    fun pastaNotasServicos(ctx: Context) = pasta(ctx, "NotasdosServicos")
    fun pastaDocumentosGerFrota(ctx: Context) = pasta(ctx, "DocumentosGerFrota")
    fun pastaFichasVeiculos(ctx: Context) = File(pastaDocumentosGerFrota(ctx), "Veiculos").apply { mkdirs() }
    fun pastaFichasMotoristas(ctx: Context) = File(pastaDocumentosGerFrota(ctx), "Motoristas").apply { mkdirs() }
    fun pastaAcertosMotoristas(ctx: Context) = File(pastaDocumentosGerFrota(ctx), "AcertosMotoristas").apply { mkdirs() }

    // --- ARQUIVO TEMPORÁRIO (câmera / OCR) ---
    fun arquivoTemporario(ctx: Context, prefixo: String, extensao: String): File =
        File(pastaCache(ctx), "${prefixo}_${System.currentTimeMillis()}.$extensao")
    
    // --- ANEXOS DE ABASTECIMENTO E ARLA ---
    fun abastecimentoCombustivel(ctx: Context) = pasta(ctx, "AbastecimentoCombustivel")
    fun abastecimentoArla(ctx: Context) = pasta(ctx, "AbastecimentoArla")

    // --- IA (Modelos GGUF) ---
    /** 
     * Modelos de IA (llama.cpp) ficam em 'filesDir' (interno) para garantir que:
     * 1. Não sejam apagados pelo usuário ao limpar o cache do app.
     * 2. O acesso via C++ (JNI) seja mais rápido e não sofra com permissões de storage.
     */
    fun pastaModelosIA(ctx: Context): File = File(ctx.filesDir, "models").apply { mkdirs() }
    
    // --- CACHE E TEMPORÁRIOS ---
    /** Usado para fotos tiradas pela câmera antes de serem salvas definitivamente ou enviadas. */
    fun pastaCache(ctx: Context): File = ctx.cacheDir
}
