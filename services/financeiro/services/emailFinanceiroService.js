/**
 * Notificações por e-mail do serviço financeiro.
 *
 * Usa nodemailer se disponível e se MAIL_USER estiver configurado; caso
 * contrário, apenas loga sem lançar — o sistema funciona sem e-mail.
 */

let transporter = null;

async function obterTransporter() {
  if (transporter) return transporter;

  const usuario = process.env.MAIL_USER;
  const senha = process.env.MAIL_PASS;
  const remetente = process.env.MAIL_FROM || usuario;

  if (!usuario || !senha) return null;

  try {
    const nodemailer = await import('nodemailer');
    transporter = nodemailer.default.createTransport({
      service: 'gmail',
      auth: { user: usuario, pass: senha },
    });
    transporter._from = remetente;
    return transporter;
  } catch {
    console.warn('[financeiro] nodemailer não instalado — e-mails desativados. Rode: npm install nodemailer');
    return null;
  }
}

async function enviar(para, assunto, html) {
  if (!para) return;
  const t = await obterTransporter();
  if (!t) return;
  try {
    await t.sendMail({ from: t._from, to: para, subject: assunto, html });
  } catch (err) {
    console.warn(`[financeiro] falha ao enviar e-mail para ${para}:`, err.message);
  }
}

const COR = '#6C63FF';

function cabecalho() {
  return `<div style="font-family:sans-serif;max-width:560px;margin:0 auto">
    <div style="background:${COR};padding:24px;border-radius:12px 12px 0 0;text-align:center">
      <h1 style="color:#fff;margin:0;font-size:22px">Mora</h1>
    </div>
    <div style="background:#1a1a24;color:#e0e0e0;padding:28px;border-radius:0 0 12px 12px">`;
}

function rodape() {
  return `</div></div>`;
}

export async function novaFatura(para, dados) {
  const { nomeUnidade, competencia, valorBRL, vencimento } = dados;
  const html = `${cabecalho()}
    <h2 style="color:#fff;margin-top:0">Nova Cobrança Disponível</h2>
    <p>Olá! Uma nova fatura foi emitida para a unidade <strong>${nomeUnidade ?? 'sua unidade'}</strong>.</p>
    <table style="width:100%;border-collapse:collapse;margin:16px 0">
      <tr><td style="padding:8px 0;color:#aaa">Competência</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#fff">${competencia}</td></tr>
      <tr><td style="padding:8px 0;color:#aaa">Valor</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#fff">${valorBRL}</td></tr>
      <tr><td style="padding:8px 0;color:#aaa">Vencimento</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#fff">${vencimento}</td></tr>
    </table>
    <p style="color:#aaa;font-size:13px">Acesse o app Mora para visualizar os detalhes e efetuar o pagamento.</p>
  ${rodape()}`;
  await enviar(para, 'Nova fatura Mora disponível', html);
}

export async function pagamentoConfirmado(para, dados) {
  const { nomeUnidade, competencia, valorBRL } = dados;
  const html = `${cabecalho()}
    <h2 style="color:#4caf50;margin-top:0">✓ Pagamento Confirmado</h2>
    <p>O pagamento da fatura da unidade <strong>${nomeUnidade ?? 'sua unidade'}</strong> foi confirmado.</p>
    <table style="width:100%;border-collapse:collapse;margin:16px 0">
      <tr><td style="padding:8px 0;color:#aaa">Competência</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#fff">${competencia}</td></tr>
      <tr><td style="padding:8px 0;color:#aaa">Valor pago</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#4caf50">${valorBRL}</td></tr>
    </table>
  ${rodape()}`;
  await enviar(para, 'Pagamento confirmado — Mora', html);
}

export async function faturaVencida(para, dados) {
  const { nomeUnidade, competencia, valorBRL } = dados;
  const html = `${cabecalho()}
    <h2 style="color:#f44336;margin-top:0">⚠ Fatura em Atraso</h2>
    <p>A fatura da unidade <strong>${nomeUnidade ?? 'sua unidade'}</strong> está em atraso.</p>
    <table style="width:100%;border-collapse:collapse;margin:16px 0">
      <tr><td style="padding:8px 0;color:#aaa">Competência</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#fff">${competencia}</td></tr>
      <tr><td style="padding:8px 0;color:#aaa">Valor</td><td style="padding:8px 0;text-align:right;font-weight:600;color:#f44336">${valorBRL}</td></tr>
    </table>
    <p style="color:#aaa;font-size:13px">Regularize o pagamento para evitar juros e multas.</p>
  ${rodape()}`;
  await enviar(para, 'Fatura em atraso — Mora', html);
}
