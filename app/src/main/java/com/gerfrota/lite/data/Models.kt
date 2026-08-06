// data/Models.kt
package com.gerfrota.lite.data

data class Veiculo(
    val id: Long? = null, val placa: String, val marca: String? = null, val modelo: String? = null,
    val cor: String? = null, val tipoVeiculo: String? = null, val anoFabricacao: String? = null,
    val anoModelo: String? = null, val renavam: String? = null, val vencLicenciamento: String? = null,
    val chassi: String? = null, val antt: String? = null, val vencAntt: String? = null,
    val carroceria: String? = null, val qtdPneus: String? = null, val observacao: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "placa" to placa, "marca" to marca, "modelo" to modelo, "cor" to cor,
        "tipo_veiculo" to tipoVeiculo, "ano_fabricacao" to anoFabricacao, "ano_modelo" to anoModelo,
        "renavam" to renavam, "vencimento_licenciamento" to vencLicenciamento, "chassi" to chassi,
        "antt" to antt, "vencimento_antt" to vencAntt, "carroceria" to carroceria,
        "quantidade_pneus" to qtdPneus, "observacao" to observacao)

    companion object {
        fun fromMap(m: Map<String, Any?>) = Veiculo(
            id = (m["id"] as? Long), placa = m["placa"]?.toString() ?: "",
            marca = m["marca"]?.toString(), modelo = m["modelo"]?.toString(),
            cor = m["cor"]?.toString(), tipoVeiculo = m["tipo_veiculo"]?.toString(),
            anoFabricacao = m["ano_fabricacao"]?.toString(), anoModelo = m["ano_modelo"]?.toString(),
            renavam = m["renavam"]?.toString(), vencLicenciamento = m["vencimento_licenciamento"]?.toString(),
            chassi = m["chassi"]?.toString(), antt = m["antt"]?.toString(),
            vencAntt = m["vencimento_antt"]?.toString(), carroceria = m["carroceria"]?.toString(),
            qtdPneus = m["quantidade_pneus"]?.toString(), observacao = m["observacao"]?.toString())
    }
}

data class Motorista(
    val id: Long? = null, val nome: String, val cpf: String? = null, val cnh: String? = null,
    val categoriaCnh: String? = null, val vencCnh: String? = null, val telefone: String? = null,
    val whatsapp: String? = null, val comissao: String? = null, val endereco: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "nome" to nome, "cpf" to cpf, "cnh" to cnh, "categoria_cnh" to categoriaCnh,
        "data_vencimento_cnh" to vencCnh, "telefone" to telefone, "whatsapp" to whatsapp,
        "comissao" to comissao, "endereco" to endereco)

    companion object {
        fun fromMap(m: Map<String, Any?>) = Motorista(
            id = (m["id"] as? Long), nome = m["nome"]?.toString() ?: "",
            cpf = m["cpf"]?.toString(), cnh = m["cnh"]?.toString(),
            categoriaCnh = m["categoria_cnh"]?.toString(), vencCnh = m["data_vencimento_cnh"]?.toString(),
            telefone = m["telefone"]?.toString(), whatsapp = m["whatsapp"]?.toString(),
            comissao = m["comissao"]?.toString(), endereco = m["endereco"]?.toString())
    }
}
