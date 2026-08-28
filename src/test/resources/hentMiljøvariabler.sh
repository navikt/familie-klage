#!/bin/bash
# Henter miljøvariabler for ApplicationLocal/ApplicationLocalPostgres med nais-cli.
# NB: ApplicationLocal forkaster første linje på stdout og leser verdiene fra linje to – behold statuslinja.

TEAM=teamfamilie
MILJO=dev-gcp
SECRET=azuread-familie-klage-lokal
BEGRUNNELSE="Lokal kjoering av familie-klage"

if [[ "$(nais device status)" != *"Connected"* ]]; then
  echo "Naisdevice er ikke tilkoblet. Start naisdevice og velg connect. Status må være grønn."
  exit 1
fi

# nais-cli skriver feilmeldinger til stdout, så vi fanger dem og viser dem videre.
if ! SECRET_JSON=$(nais secret get "$SECRET" -e "$MILJO" -t "$TEAM" \
  --with-values --reason "$BEGRUNNELSE" -o json 2>&1); then
  printf '%s\n' "Klarte ikke hente secreten $SECRET:" "$SECRET_JSON" \
    "Er du på naisdevice og logget inn med 'nais login -y'?"
  exit 1
fi

AZURE_KV=$(printf '%s\n' "$SECRET_JSON" | jq -r '.data[] | "\(.key)=\(.value)"')

# Alle fire må være med: ApplicationLocal splitter på ';' og indekserer [1] uten sjekk,
# så en manglende verdi ville gitt IndexOutOfBounds i stedet for en lesbar feilmelding.
FELTER=()
MANGLER=""
for NOKKEL in AZURE_APP_CLIENT_ID AZURE_APP_CLIENT_SECRET AZURE_OPENID_CONFIG_ISSUER AZURE_OPENID_CONFIG_JWKS_URI; do
  LINJE=$(printf '%s\n' "$AZURE_KV" | grep "^$NOKKEL=" | head -1)
  if [[ -z "$LINJE" ]]; then
    MANGLER="$MANGLER $NOKKEL"
  else
    FELTER+=("$LINJE")
  fi
done

if [[ -n "$MANGLER" ]]; then
  printf '%s\n' "Manglet følgende nøkler i $SECRET:$MANGLER"
  exit 1
fi

echo "Henter miljøvariabler med nais-cli..."
printf '%s;%s;%s;%s' "${FELTER[0]}" "${FELTER[1]}" "${FELTER[2]}" "${FELTER[3]}"
