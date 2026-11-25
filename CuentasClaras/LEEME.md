# Cuentas claras · LOR76 — App Android

Reparte gastos de grupo, chat de grupo y privado, y escáner de precios con la cámara.
Funciona en Android 5.0 o superior.

---

## Paso 1 · Crear la base de datos en la nube (una sola vez, gratis)

Sirve para que **todos los móviles del grupo vean lo mismo**. Por ejemplo, si cargas 1 € a Juanito, a él le aparece en su móvil.

1. Entra en **https://console.firebase.google.com** con tu cuenta de Google.
2. Pulsa **«Crear un proyecto»** y ponle un nombre, por ejemplo `cuentas-claras`. Puedes desactivar Google Analytics. Pulsa **Crear**.
3. En el menú de la izquierda, entra en **Databases & Storage** (o «Bases de datos y almacenamiento») **→ Realtime Database** y pulsa **«Crear base de datos»**.
   Atajo: abre https://console.firebase.google.com/project/_/database y elige tu proyecto.
4. Elige la ubicación **Bélgica (europe-west1)** y pulsa **Siguiente**.
5. Elige **«Comenzar en modo de prueba»** y pulsa **Habilitar**.
6. Arriba verás una dirección parecida a esta:
   `https://cuentas-claras-1234-default-rtdb.europe-west1.firebasedatabase.app`
   **Cópiala.**
7. Abre la pestaña **Reglas**, borra lo que haya, pega esto y pulsa **Publicar**:
   ```json
   {
     "rules": {
       ".read": true,
       ".write": true
     }
   }
   ```
   (El modo de prueba normal caduca a los 30 días. Con estas reglas no caduca.)

> ⚠️ Cualquiera que conozca esa dirección puede leer y escribir en la base de datos.
> Compártela solo a través de la app y no guardes en ella nada delicado.

## Paso 2 · Poner la dirección en la app

Abre el archivo `app/src/main/assets/config.js` con el Bloc de notas y pega tu dirección entre las comillas:

```js
window.FIREBASE_URL = "https://cuentas-claras-1234-default-rtdb.europe-west1.firebasedatabase.app";
```

Guarda el archivo. Así todos los que instalen tu APK se conectan solos.
También se puede cambiar después desde **Ajustes**, dentro de la app.

## Paso 3 · Compilar el APK con GitHub (como con el descargador)

1. Entra en **https://github.com** → botón **New** (nuevo repositorio) → nombre `CuentasClaras` → **Create repository**.
2. Pulsa **«uploading an existing file»**, arrastra **todo el contenido** de esta carpeta (incluida la carpeta `.github`) y pulsa **Commit changes**.
3. Ve a la pestaña **Actions**. Verás **«Compilar APK»** en marcha (tarda unos 5 minutos).
4. Cuando salga el tic verde, entra en la compilación y descarga **CuentasClaras-LOR76** (abajo, en *Artifacts*). Dentro está `CuentasClaras-LOR76.apk`.
5. Pásalo al móvil (o a BlueStacks) e instálalo. Android te pedirá permitir «instalar apps de origen desconocido».

## Cómo se usa

- **Al abrirla por primera vez**, cada persona pone su nombre y su **número de teléfono**.
- **Nuevo grupo → «Añadir desde mis contactos»**: elige a Juanito de la agenda. La app guarda su número.
- Cuando Juanito instale la app y se registre con ese número, **el grupo le aparece solo**, con lo que ha gastado y lo que debe.
- El botón **«Invitar»**, junto a cada persona que aún no se ha unido, le envía un mensaje por WhatsApp, SMS, etc.
- **Saldos**: muestra quién paga a quién. Con **«Pagado»** se apunta que una deuda está saldada.
- **Precios → Escanear**: hazle una foto a un ticket. La app lee los productos y precios (sin internet, dentro del móvil). Puedes corregirlos y convertirlos en un gasto.

## Archivos

| Archivo | Qué es |
|---|---|
| `app/src/main/assets/index.html` | Toda la interfaz (estética LOR76) |
| `app/src/main/assets/config.js` | Dirección de la base de datos |
| `app/src/main/java/.../MainActivity.java` | Cámara, lector de tickets, contactos y compartir |
| `app/src/main/res/mipmap-*` | Icono LOR76 |
| `.github/workflows/compilar.yml` | Receta para que GitHub compile el APK |
