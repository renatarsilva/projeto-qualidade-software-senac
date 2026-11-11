#!/usr/bin/env python3
"""Calcula p50, p95, p99, vazão e erros de uma rodada do JMeter (.jtl em CSV), separando o 409 esperado do erro real.
Uso: perf/analisar.py perf/resultados/rodada-01.jtl"""
import csv
import statistics
import sys
from collections import Counter, defaultdict


def percentil(valores, p):
    ordenados = sorted(valores)
    posicao = max(0, min(len(ordenados) - 1, int(round(p / 100 * len(ordenados) + 0.5)) - 1))
    return ordenados[posicao]


tempos = defaultdict(list)
codigos = defaultdict(Counter)
inicio = fim = None
with open(sys.argv[1], newline="", encoding="utf-8") as arquivo:
    for linha in csv.DictReader(arquivo):
        rotulo = linha["label"]
        tempos[rotulo].append(int(linha["elapsed"]))
        codigos[rotulo][linha["responseCode"]] += 1
        marco = int(linha["timeStamp"])
        inicio = marco if inicio is None else min(inicio, marco)
        fim = marco + int(linha["elapsed"]) if fim is None else max(fim, marco + int(linha["elapsed"]))

duracao = (fim - inicio) / 1000
print(f"Duração da rodada: {duracao:.1f} s")
for rotulo, lista in tempos.items():
    total = len(lista)
    esperados = {"disponibilidade": {"200"}, "criacao-disputada": {"201", "409"}, "login": {"200"}}.get(rotulo, {"200"})
    erro_real = sum(c for cod, c in codigos[rotulo].items() if cod not in esperados)
    print(f"\n{rotulo}: {total} amostras, {total / duracao:.1f} req/s")
    print(f"  p50 {percentil(lista, 50)} ms | p95 {percentil(lista, 95)} ms | p99 {percentil(lista, 99)} ms "
          f"| média {statistics.mean(lista):.0f} ms | máx {max(lista)} ms")
    print(f"  códigos: {dict(codigos[rotulo])}")
    print(f"  erro real: {erro_real} ({100 * erro_real / total:.2f} %)")
