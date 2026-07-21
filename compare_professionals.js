// ============================================
// COMPARACIÓN DE COLECCIONES (SOLO LECTURA)
// Compara Professionals_PVT vs Professionals
// ============================================

print('🔍 Iniciando comparación de colecciones...\n');

// Configura las colecciones
var collectionPVT = db.getCollection('Professionals_PVT');
var collectionProd = db.getCollection('Professionals');

print('═══════════════════════════════════════════');
print('📊 ANÁLISIS DE COLECCIONES');
print('═══════════════════════════════════════════\n');

// Obtén los IDs de ambas colecciones
var idsPVT = collectionPVT.distinct('_id');
var idsProd = collectionProd.distinct('_id');

print('📦 Professionals_PVT: ' + idsPVT.length + ' documentos');
print('📦 Professionals: ' + idsProd.length + ' documentos');
print('📊 Diferencia: ' + (idsPVT.length - idsProd.length) + ' documentos\n');

// Encuentra documentos que están en PVT pero NO en Professionals
var missingInProd = idsPVT.filter(function(id) {
    return idsProd.indexOf(id) === -1;
});

// Encuentra documentos que están en Professionals pero NO en PVT
var missingInPVT = idsProd.filter(function(id) {
    return idsPVT.indexOf(id) === -1;
});

// Encuentra documentos que están en ambas (intersección)
var inBoth = idsPVT.filter(function(id) {
    return idsProd.indexOf(id) !== -1;
});

print('═══════════════════════════════════════════');
print('📋 RESUMEN DE DIFERENCIAS');
print('═══════════════════════════════════════════\n');

print('✅ Documentos en ambas colecciones: ' + inBoth.length);
print('⚠️  Solo en Professionals_PVT: ' + missingInProd.length);
print('⚠️  Solo en Professionals: ' + missingInPVT.length + '\n');

// Detalle de documentos que faltan en Professionals
if (missingInProd.length > 0) {
    print('═══════════════════════════════════════════');
    print('📝 DOCUMENTOS QUE FALTAN EN PROFESSIONALS');
    print('   (presentes en Professionals_PVT)');
    print('═══════════════════════════════════════════\n');

    missingInProd.forEach(function(id, index) {
        var doc = collectionPVT.findOne({_id: id});
        if (doc) {
            var displayName = '';
            if (doc.name) {
                displayName = ' → ' + doc.name + ' ' + (doc.surname || '');
            }
            if (doc.email) {
                displayName += ' (' + doc.email + ')';
            }
            print('[' + (index + 1) + '] ' + id + displayName);
        } else {
            print('[' + (index + 1) + '] ' + id + ' (error al leer)');
        }
    });
    print('');
}

// Detalle de documentos que faltan en Professionals_PVT
if (missingInPVT.length > 0) {
    print('═══════════════════════════════════════════');
    print('📝 DOCUMENTOS QUE FALTAN EN PROFESSIONALS_PVT');
    print('   (presentes en Professionals)');
    print('═══════════════════════════════════════════\n');

    missingInPVT.forEach(function(id, index) {
        var doc = collectionProd.findOne({_id: id});
        if (doc) {
            var displayName = '';
            if (doc.name) {
                displayName = ' → ' + doc.name + ' ' + (doc.surname || '');
            }
            if (doc.email) {
                displayName += ' (' + doc.email + ')';
            }
            print('[' + (index + 1) + '] ' + id + displayName);
        } else {
            print('[' + (index + 1) + '] ' + id + ' (error al leer)');
        }
    });
    print('');
}

// Comparación de campos en documentos comunes (muestra de 5)
if (inBoth.length > 0) {
    print('═══════════════════════════════════════════');
    print('🔬 VERIFICACIÓN DE CONTENIDO');
    print('   (muestra de hasta 5 documentos comunes)');
    print('═══════════════════════════════════════════\n');

    var sampleSize = Math.min(5, inBoth.length);
    var differencesFound = 0;

    for (var i = 0; i < sampleSize; i++) {
        var id = inBoth[i];
        var docPVT = collectionPVT.findOne({_id: id});
        var docProd = collectionProd.findOne({_id: id});

        var pvtJson = JSON.stringify(docPVT, null, 2);
        var prodJson = JSON.stringify(docProd, null, 2);

        if (pvtJson !== prodJson) {
            differencesFound++;
            print('⚠️  Diferencias en documento: ' + id);
            if (docPVT.name) {
                print('   Nombre: ' + docPVT.name + ' ' + (docPVT.surname || ''));
            }
            print('   Los contenidos difieren entre colecciones\n');
        }
    }

    if (differencesFound === 0) {
        print('✅ Los ' + sampleSize + ' documentos verificados son idénticos\n');
    } else {
        print('⚠️  Se encontraron ' + differencesFound + ' documentos con diferencias de contenido\n');
    }
}

print('═══════════════════════════════════════════');
print('📊 ESTADÍSTICAS FINALES');
print('═══════════════════════════════════════════\n');

print('📦 Total Professionals_PVT: ' + idsPVT.length);
print('📦 Total Professionals: ' + idsProd.length);
print('✅ Sincronizados: ' + inBoth.length);
print('⚠️  Faltan en Professionals: ' + missingInProd.length);
print('⚠️  Faltan en Professionals_PVT: ' + missingInPVT.length);

var syncPercentage = ((inBoth.length / Math.max(idsPVT.length, idsProd.length)) * 100).toFixed(2);
print('📊 Porcentaje de sincronización: ' + syncPercentage + '%\n');

if (missingInProd.length === 0 && missingInPVT.length === 0) {
    print('✅ Las colecciones están completamente sincronizadas.\n');
} else {
    print('⚠️  Las colecciones NO están sincronizadas.\n');
    if (missingInProd.length > 0) {
        print('   💡 Ejecuta el script de sincronización para copiar ' + missingInProd.length + ' documentos a Professionals\n');
    }
}

print('✅ Comparación completada.\n');
