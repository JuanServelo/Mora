import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO } from '../middleware/auth.js';
import { resolverEscopo } from '../middleware/escopo.js';
import { ASAAS, gatewayConfigurado } from '../config/asaas.js';
import {
  garantirCliente, criarCobranca, buscarPixQrCode,
  buscarLinhaDigitavel, consultarCobranca, cancelarCobranca,
} from '../gateway/asaasClient.js';
import { paraCentavos, formatarBRL } from '../utils/dinheiro.js';

const router = express.Router();

const adm = [autenticar, exigirPerfis(...PERFIS_GESTAO), resolverEscopo];

/**
 * Só opera contra o ambiente de teste do gateway.
 *
 * Esta rota emite cobrança de verdade. Em produção isso significaria mandar um
 * boleto real para alguém, e o botão está numa tela — a proteção não pode
 * depender de ninguém lembrar de não clicar.
 */
function ehSandbox() {
  return /sandbox/i.test(ASAAS.baseUrl);
}

/** CPF com dígitos verificadores válidos — o gateway recusa CPF inventado. */
function cpfDeTeste(base = '249715637') {
  const dv = (nums) => {
    const peso = nums.length + 1;
    const soma = nums.reduce((a, n, i) => a + n * (peso - i), 0);
    const r = (soma * 10) % 11;
    return r === 10 ? 0 : r;
  };
  const n = base.split('').map(Number);
  const d1 = dv(n);
  return base + d1 + dv([...n, d1]);
}

/** Vencimento daqui a alguns dias, no formato que o gateway espera. */
function vencimentoEm(dias = 7) {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return d.toISOString().slice(0, 10);
}

router.get('/gateway/status', adm, (_req, res) => {
  res.json({
    sucesso: true,
    configurado: gatewayConfigurado(),
    sandbox: ehSandbox(),
    baseUrl: ASAAS.baseUrl,
    // A chave nunca sai daqui. Só o suficiente para o síndico conferir que é a
    // que ele cadastrou.
    ...(gatewayConfigurado() && { chaveFinal: ASAAS.apiKey.slice(-6) }),
  });
});

router.post('/gateway/teste', adm, async (req, res) => {
  if (!gatewayConfigurado()) {
    return res.status(503).json({
      sucesso: false,
      mensagem: 'Gateway sem credencial. Defina ASAAS_API_KEY.',
    });
  }
  if (!ehSandbox()) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'O teste de cobrança só roda contra o sandbox do gateway.',
    });
  }

  const forma = req.body?.forma === 'BOLETO' ? 'BOLETO' : 'PIX';
  const valorCentavos = paraCentavos(req.body?.valor ?? '350,00');
  if (valorCentavos === null || valorCentavos < 500) {
    return res.status(400).json({
      sucesso: false,
      mensagem: 'Informe um valor válido de no mínimo R$ 5,00.',
    });
  }

  const cliente = await garantirCliente({
    nome: 'Pagador de teste — Mora',
    cpf: cpfDeTeste(),
    email: 'teste@qa.mora.local',
    usuarioId: req.escopo.usuarioId,
  });
  if (!cliente.ok) {
    return res.status(502).json({ sucesso: false, mensagem: `Gateway: ${cliente.erro}` });
  }

  const vencimento = vencimentoEm(7);
  const cobranca = await criarCobranca({
    clienteId: cliente.dados.id,
    forma,
    valorCentavos,
    vencimento,
    descricao: `Teste de integração — ${req.escopo.condominioId}`,
    // Não é uma fatura real: o prefixo deixa isso claro em qualquer relatório
    // do gateway, e impede que o webhook procure uma fatura que não existe.
    faturaId: `teste-${Date.now()}`,
  });
  if (!cobranca.ok) {
    return res.status(502).json({ sucesso: false, mensagem: `Gateway: ${cobranca.erro}` });
  }

  const resposta = {
    sucesso: true,
    cobranca: {
      id: cobranca.dados.id,
      forma,
      status: cobranca.dados.status,
      valorCentavos,
      valorFormatado: formatarBRL(valorCentavos),
      vencimento,
      linkPagamento: cobranca.dados.invoiceUrl ?? null,
    },
  };

  if (forma === 'PIX') {
    const qr = await buscarPixQrCode(cobranca.dados.id);
    if (qr.ok) {
      resposta.pix = { payload: qr.dados.payload, imagem: qr.dados.encodedImage };
    } else {
      // A causa mais comum é a conta não ter chave PIX cadastrada. Dizer isso
      // poupa o síndico de procurar erro na integração.
      resposta.aviso = `PIX indisponível: ${qr.erro}`;
    }
  } else {
    const linha = await buscarLinhaDigitavel(cobranca.dados.id);
    resposta.boleto = {
      url: cobranca.dados.bankSlipUrl ?? null,
      linhaDigitavel: linha.ok ? linha.dados.identificationField : null,
    };
    if (!linha.ok) resposta.aviso = `Linha digitável indisponível: ${linha.erro}`;
  }

  res.status(201).json(resposta);
});

router.get('/gateway/teste/:id', adm, async (req, res) => {
  const r = await consultarCobranca(req.params.id);
  if (!r.ok) return res.status(502).json({ sucesso: false, mensagem: r.erro });
  res.json({
    sucesso: true,
    cobranca: {
      id: r.dados.id,
      status: r.dados.status,
      deletada: r.dados.deleted,
      pagoEm: r.dados.paymentDate ?? null,
    },
  });
});

router.delete('/gateway/teste/:id', adm, async (req, res) => {
  const r = await cancelarCobranca(req.params.id);
  if (!r.ok) return res.status(502).json({ sucesso: false, mensagem: r.erro });
  res.json({ sucesso: true });
});

export default router;
