#!/usr/bin/env python3
"""Pruebas de infra/scripts/ubigeo-a-sql (SPEC-0a sección 6: V2__ubigeo_inei.sql regenerable).

Uso:  python3 -I infra/scripts/ubigeo-a-sql-prueba.py
Los casos usan la muestra pequeña ubigeo-a-sql-muestra.csv y archivos temporales; no tocan la migración real.
"""
import pathlib
import re
import subprocess
import sys
import tempfile
import unittest

AQUI = pathlib.Path(__file__).resolve().parent
SCRIPT = AQUI / "ubigeo-a-sql"
MUESTRA = AQUI / "ubigeo-a-sql-muestra.csv"
DATOS = AQUI / "ubigeo-a-sql-datos" / "ubigeo_distrito.csv"
CORRECCIONES = AQUI / "ubigeo-a-sql-datos" / "correcciones.csv"


def ejecutar(*argumentos):
    return subprocess.run([sys.executable, "-I", str(SCRIPT), *map(str, argumentos)],
                          capture_output=True, text=True, encoding="utf-8")


def csv_temporal(contenido, codificacion="utf-8"):
    carpeta = tempfile.mkdtemp(prefix="ubigeo-prueba-")
    ruta = pathlib.Path(carpeta) / "entrada.csv"
    ruta.write_bytes(contenido.encode(codificacion))
    return ruta


class UbigeoASqlTest(unittest.TestCase):

    # CA-0a-05: V2 se genera desde el CSV del INEI con un script versionado
    def test_genera_departamentos_provincias_y_distritos(self):
        r = ejecutar(MUESTRA)
        self.assertEqual(r.returncode, 0, r.stderr)
        sql = r.stdout
        self.assertIn("INSERT INTO departamento (codigo, nombre) VALUES", sql)
        self.assertIn("('15', 'Lima')", sql)
        self.assertIn("('04', 'Arequipa')", sql)
        self.assertIn("('10', 'Huánuco')", sql)
        self.assertIn("INSERT INTO provincia (codigo, departamento_codigo, nombre) VALUES", sql)
        self.assertIn("('1501', '15', 'Lima')", sql)
        self.assertIn("('1502', '15', 'Barranca')", sql)
        self.assertIn("INSERT INTO distrito (codigo, provincia_codigo, nombre) VALUES", sql)
        self.assertIn("('150122', '1501', 'Miraflores')", sql)
        self.assertIn("('040117', '0401', 'Miraflores')", sql)

    # CA-0a-05
    def test_cuenta_cada_nivel_una_sola_vez(self):
        sql = ejecutar(MUESTRA).stdout
        self.assertEqual(sql.count("('15', 'Lima')"), 1)
        self.assertEqual(sql.count("('1501', '15', 'Lima')"), 1)
        # 5 departamentos, 6 provincias y 10 distritos en la muestra
        self.assertIn("-- departamentos: 5, provincias: 6, distritos: 10", sql)

    # CA-0a-05
    def test_orden_por_codigo_y_resultado_repetible(self):
        a = ejecutar(MUESTRA).stdout
        b = ejecutar(MUESTRA).stdout
        self.assertEqual(a, b)
        self.assertLess(a.index("('04', 'Arequipa')"), a.index("('15', 'Lima')"))
        self.assertLess(a.index("('040101'"), a.index("('150101'"))

    # CA-0a-05
    def test_escapa_comillas_simples(self):
        sql = ejecutar(MUESTRA).stdout
        self.assertIn("'Distrito O''Higgins'", sql)

    # CA-0a-05
    def test_encabezado_indica_que_es_generado_y_su_fuente(self):
        sql = ejecutar(MUESTRA).stdout
        self.assertIn("GENERADO por infra/scripts/ubigeo-a-sql", sql)
        self.assertIn("ubigeo-a-sql-muestra.csv", sql)
        self.assertIsNone(re.search(r"\d{4}-\d{2}-\d{2}", sql))  # sin fecha: la salida no cambia entre ejecuciones

    # CA-0a-05
    def test_escribe_a_un_archivo_con_la_opcion_salida(self):
        destino = pathlib.Path(tempfile.mkdtemp(prefix="ubigeo-salida-")) / "V2.sql"
        r = ejecutar(MUESTRA, "--salida", destino)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("INSERT INTO distrito", destino.read_text(encoding="utf-8"))
        self.assertEqual(r.stdout, "")

    # CA-0a-05
    def test_lee_punto_y_coma_y_codificacion_latin1(self):
        ruta = csv_temporal("UBIGEO;DEPARTAMENTO;PROVINCIA;DISTRITO\n100101;Huánuco;Huánuco;Huánuco\n",
                            "latin-1")
        r = ejecutar(ruta, "--codificacion", "latin-1")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("('100101', '1001', 'Huánuco')", r.stdout)

    # CA-0a-05
    def test_acepta_el_codigo_con_comillas_y_ceros_a_la_izquierda(self):
        ruta = csv_temporal('"UBIGEO","DEPARTAMENTO","PROVINCIA","DISTRITO"\n"010101","Amazonas","Chachapoyas","Chachapoyas"\n')
        r = ejecutar(ruta)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("('01', 'Amazonas')", r.stdout)
        self.assertIn("('010101', '0101', 'Chachapoyas')", r.stdout)

    # CA-0a-05
    def test_rechaza_un_codigo_que_no_tiene_seis_digitos(self):
        ruta = csv_temporal("UBIGEO,DEPARTAMENTO,PROVINCIA,DISTRITO\n15012,Lima,Lima,Miraflores\n")
        r = ejecutar(ruta)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("15012", r.stderr)

    # CA-0a-05
    def test_rechaza_nombres_distintos_para_el_mismo_departamento(self):
        ruta = csv_temporal("UBIGEO,DEPARTAMENTO,PROVINCIA,DISTRITO\n150101,Lima,Lima,Lima\n150122,Limaa,Lima,Miraflores\n")
        r = ejecutar(ruta)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("departamento", r.stderr.lower())

    # CA-0a-05
    def test_rechaza_un_distrito_repetido(self):
        ruta = csv_temporal("UBIGEO,DEPARTAMENTO,PROVINCIA,DISTRITO\n150101,Lima,Lima,Lima\n150101,Lima,Lima,Lima\n")
        r = ejecutar(ruta)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("150101", r.stderr)

    # CA-0a-05
    def test_rechaza_un_csv_sin_las_columnas_esperadas(self):
        ruta = csv_temporal("CODIGO,NOMBRE\n150101,Lima\n")
        r = ejecutar(ruta)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("UBIGEO", r.stderr)

    # CA-0a-05
    def test_rechaza_un_csv_vacio_y_un_archivo_inexistente(self):
        r = ejecutar(csv_temporal("UBIGEO,DEPARTAMENTO,PROVINCIA,DISTRITO\n"))
        self.assertNotEqual(r.returncode, 0)
        r = ejecutar(AQUI / "no-existe.csv")
        self.assertNotEqual(r.returncode, 0)

    # CA-0a-05: el CSV derivado del INEI usa la columna "inei" y trae columnas de más que se ignoran
    def test_acepta_la_columna_inei_e_ignora_las_demas(self):
        ruta = csv_temporal("inei,reniec,departamento,provincia,distrito,region\n"
                            "150122,150122,LIMA,LIMA,MIRAFLORES,LIMA\n")
        r = ejecutar(ruta)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("('150122', '1501', 'MIRAFLORES')", r.stdout)

    # CA-0a-05: conteos del INEI sobre los datos versionados
    def test_datos_versionados_tienen_los_conteos_del_inei(self):
        r = ejecutar(DATOS, "--correcciones", CORRECCIONES)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("('180107', '1801', 'SAN ANTONIO')", r.stdout)
        self.assertIn("-- departamentos: 25, provincias: 196, distritos: 1895", r.stdout)
        self.assertIn("('150122', '1501', 'MIRAFLORES')", r.stdout)
        self.assertIn("('150115', '1501', 'LA VICTORIA')", r.stdout)

    # CA-0a-05: un código faltante en la fuente se completa solo con una corrección explícita
    def test_sin_correccion_un_codigo_faltante_se_rechaza(self):
        r = ejecutar(DATOS)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("SAN ANTONIO", r.stderr)

    # CA-0a-05
    def test_aplica_la_correccion_por_nombres(self):
        ruta = csv_temporal("inei,departamento,provincia,distrito\nNA,MOQUEGUA,MARISCAL NIETO,SAN ANTONIO\n")
        corr = csv_temporal("departamento,provincia,distrito,inei\nMOQUEGUA,MARISCAL NIETO,SAN ANTONIO,180107\n")
        r = ejecutar(ruta, "--correcciones", corr)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("('180107', '1801', 'SAN ANTONIO')", r.stdout)


if __name__ == "__main__":
    unittest.main()
