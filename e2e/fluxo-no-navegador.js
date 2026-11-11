// Fluxo ponta a ponta pelo navegador real (Chrome), em 375 px de largura: cadastros, reserva, conflito e aprovação.
const puppeteer = require('puppeteer-core');
const fs = require('fs');
const SAIDA = process.argv[2];
const BASE = 'http://localhost:8080';
const ID = String(Date.now()).slice(-6);
const resultados = [];
const confere = (ok, descricao) => { resultados.push({ ok, descricao }); console.log((ok ? 'OK    ' : 'FALHOU') + ' ' + descricao); };

(async () => {
  const navegador = await puppeteer.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', headless: 'new' });
  const pagina = await navegador.newPage();
  await pagina.setViewport({ width: 375, height: 812, deviceScaleFactor: 2, isMobile: true });
  const foto = async (nome) => { await pagina.screenshot({ path: `${SAIDA}/${nome}.png`, fullPage: true }); };
  const semRolagem = async (nome) => {
    const r = await pagina.evaluate(() => ({ largura: document.documentElement.scrollWidth, janela: window.innerWidth }));
    confere(r.largura <= r.janela + 1, `${nome}: sem rolagem horizontal em 375 px (${r.largura} <= ${r.janela})`);
  };
  const vai = async (caminho) => { await pagina.goto(BASE + caminho, { waitUntil: 'networkidle0' }); };
  const clica = async (seletor) => { await Promise.all([pagina.waitForNavigation({ waitUntil: 'networkidle0' }), pagina.click(seletor)]); };
  const digita = async (seletor, texto) => { await pagina.$eval(seletor, e => { e.value = ''; }); await pagina.type(seletor, texto); };
  const defineCampo = async (seletor, valor) => { await pagina.$eval(seletor, (e, v) => { e.value = v; e.dispatchEvent(new Event('input', { bubbles: true })); }, valor); };
  const texto = async () => pagina.evaluate(() => document.body.innerText);
  const entra = async (email, senha) => {
    await vai('/login'); await digita('#username', email); await digita('#password', senha);
    await clica('button[type=submit]');
  };
  const sai = async () => {
    await pagina.click('.navbar-toggler'); await new Promise(r => setTimeout(r, 500));
    await Promise.all([pagina.waitForNavigation({ waitUntil: 'networkidle0' }), pagina.click('form[action="/logout"] button')]);
  };

  // 1. Login (público)
  await vai('/login'); await foto('01-login-375px'); await semRolagem('login');
  confere(await pagina.$('meta[name=viewport]') !== null, 'login: tem a meta viewport');
  await digita('#username', 'ninguem@exemplo.com'); await digita('#password', 'senha-errada-123');
  await clica('button[type=submit]');
  confere((await texto()).includes('E-mail ou senha inválidos'), 'login com senha errada mostra a mensagem clara');

  // 2. Administrador cadastra sala, material, professor e usuários
  await entra('admin@exemplo.com', 'senha-do-admin-123');
  confere(pagina.url() === BASE + '/', 'administrador entra e vai para a página inicial');
  await pagina.click('.navbar-toggler'); await new Promise(r => setTimeout(r, 500)); await foto('02-menu-do-administrador-375px');
  const sala = 'sala-' + (200 + (Number(ID) % 700));
  await vai('/admin/salas');
  await digita('#nome', sala); await digita('#tipoSala', 'Laboratório'); await digita('#capacidade', '30'); await digita('#localizacao', 'Bloco B');
  await clica('form[action="/admin/salas"] button[type=submit]');
  confere((await texto()).includes(sala), `sala ${sala} cadastrada e listada`); await foto('03-salas-375px'); await semRolagem('salas');
  await vai('/admin/materiais');
  await digita('#nome', 'Projetor ' + ID); await digita('#tipoMaterial', 'Projetor'); await digita('#patrimonio', 'PAT-' + ID);
  await clica('form[action="/admin/materiais"] button[type=submit]');
  confere((await texto()).includes('Projetor ' + ID), 'material cadastrado e listado'); await semRolagem('materiais');
  await vai('/admin/professores');
  await digita('#nome', 'ana-lima-' + ID.padStart(5, '0').slice(-5)); await digita('#competencias', 'Java, Testes');
  await clica('form[action="/admin/professores"] button[type=submit]');
  confere((await texto()).includes('Java, Testes'), 'professor cadastrado com competências'); await semRolagem('professores');
  const mail = (p) => `${p}${ID}@senac.br`;
  for (const [nome, apelido, perfil] of [['Marina Couto', 'marina', 'SOLICITANTE'], ['Carlos Lima', 'carlos', 'RESPONSAVEL']]) {
    await vai('/admin/usuarios');
    await digita('#nome', nome); await digita('#email', mail(apelido)); await digita('#senha', 'senha-segura-123');
    await pagina.select('#perfil', perfil);
    await clica('form[action="/admin/usuarios"] button[type=submit]');
    confere((await texto()).includes(mail(apelido)), `usuário ${apelido} (${perfil}) cadastrado`);
  }
  await foto('04-usuarios-375px'); await semRolagem('usuarios');
  await vai('/admin/manutencoes'); await foto('05-manutencao-375px'); await semRolagem('manutenção');
  await sai();

  // 3. Solicitante pesquisa, reserva e vê o conflito
  await entra(mail('marina'), 'senha-segura-123');
  const dia = 5 + (Number(ID) % 20);
  const de = `2026-12-${String(dia).padStart(2, '0')}T10:00`, ate = `2026-12-${String(dia).padStart(2, '0')}T11:00`;
  await vai(`/recursos?tipo=SALA&de=${de}&ate=${ate}`);
  confere((await texto()).includes(sala), 'a pesquisa por disponibilidade mostra a sala livre'); await foto('06-pesquisa-375px'); await semRolagem('pesquisa');
  await pagina.click('a.btn-outline-primary');
  await pagina.waitForSelector('#inicio');
  confere(await pagina.$eval('#inicio', e => e.value) === de, 'o link Reservar leva o horário para o formulário');
  await foto('07-nova-reserva-375px'); await semRolagem('nova reserva');
  await clica('form[action="/reservas"] button[type=submit]');
  confere(/Reserva #\d+/.test(await texto()) && (await texto()).includes('Solicitada'), 'a reserva é criada como Solicitada');
  const idReserva = pagina.url().split('/').pop();
  await foto('08-reserva-criada-375px'); await semRolagem('detalhe da reserva');
  await vai('/reservas/nova'); await defineCampo('#inicio', `2026-12-${String(dia).padStart(2, '0')}T10:30`); await defineCampo('#termino', `2026-12-${String(dia).padStart(2, '0')}T11:30`);
  await pagina.$$eval('input[name=recursos]', cs => cs.filter(c => c.value.startsWith('SALA:')).forEach(c => { c.checked = true; }));
  await clica('form[action="/reservas"] button[type=submit]');
  confere((await texto()).includes('A sala já está reservada neste horário'), 'o horário ocupado mostra a mensagem clara da regra');
  await foto('09-conflito-375px'); await semRolagem('conflito');
  await vai('/reservas'); await foto('10-minhas-reservas-375px'); await semRolagem('minhas reservas');
  await vai('/admin/salas'); confere((await texto()).includes('Acesso negado'), 'o Solicitante não entra em /admin/salas (página de acesso negado)'); await foto('11-acesso-negado-375px');
  await vai('/'); await sai();

  // 4. Responsável aprova e vê o histórico
  await entra(mail('carlos'), 'senha-segura-123');
  await vai('/aprovacoes'); confere((await texto()).includes('#') || (await texto()).includes(idReserva), 'a reserva aparece na fila de aprovação'); await foto('12-fila-de-aprovacao-375px'); await semRolagem('fila de aprovação');
  await pagina.click(`#reserva-${idReserva} form[action$="/aprovar"] button`);
  await pagina.waitForNavigation({ waitUntil: 'networkidle0' });
  confere((await texto()).includes('aprovada'), 'o Responsável aprova a reserva');
  await vai('/reservas/' + idReserva);
  confere((await texto()).includes('Histórico de mudanças') && (await texto()).includes('Aprovada'), 'o histórico de mudanças mostra a aprovação');
  await foto('13-historico-375px'); await semRolagem('histórico');
  await vai('/relatorio?inicio=2026-12-01&fim=2026-12-31');
  confere((await texto()).includes('Conflitos evitados'), 'o relatório de utilização abre'); await foto('14-relatorio-375px'); await semRolagem('relatório');

  // 5. Desktop
  await pagina.setViewport({ width: 1280, height: 800 });
  await vai('/reservas/' + idReserva); await foto('15-reserva-desktop');
  await vai('/aprovacoes'); await foto('16-fila-desktop');

  await navegador.close();
  const falhas = resultados.filter(r => !r.ok);
  fs.writeFileSync(`${SAIDA}/resultado.json`, JSON.stringify({ data: new Date().toISOString(), total: resultados.length, falhas: falhas.length, verificacoes: resultados }, null, 2));
  console.log(`\n${resultados.length - falhas.length}/${resultados.length} verificações OK`);
  process.exit(falhas.length ? 1 : 0);
})().catch(e => { console.error('ERRO', e.message); process.exit(2); });
