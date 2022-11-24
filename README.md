# familie-klage
App for behandling av klager (team familie)

## Bygging lokalt
Appen kjører på JRE 11. Bygging gjøres ved å kjøre `mvn clean install`.

### Autentisering lokalt
Dersom man vil gjøre autentiserte kall mot andre tjenester eller vil kjøre applikasjonen sammen med frontend, må man sette opp følgende miljø-variabler:

#### Client id & client secret
secret kan hentes fra cluster med
`kubectl -n teamfamilie get secret azuread-familie-klage-lokal -o json | jq '.data | map_values(@base64d)'`
`kubectl -n teamfamilie get secret azuread-familie-klage-frontend-lokal -o json | jq '.data | map_values(@base64d)'`

* `AZURE_APP_CLIENT_ID` (fra secret)
* `AZURE_APP_CLIENT_SECRET` (fra secret)
* Scope for den aktuelle tjenesten (`FAMILIE_INTEGRASJONER_SCOPE`, `FAMILIE_OPPDRAG_SCOPE`, `EF_INFOTRYGD_FEED_SCOPE`, `EF_INFOTRYGD_REPLIKA_SCOPE`)

Variablene legges inn under ApplicationLocal -> Edit Configurations -> Environment Variables.

### Kjøring med in-memory-database
For å kjøre opp appen lokalt, kan en kjøre `ApplicationLocal`.

Appen starter da opp med en in memory-database og er da tilgjengelig under `localhost:8093`.
Databasen kan aksesseres på `localhost:8093/h2-console`. Log på jdbc url `jdbc:h2:mem:testdb` med bruker `sa` og blankt passord.

### Kjøring med postgres-database
For å kjøre opp appen lokalt med en postgres-database, kan en kjøre `ApplicationLocalPostgres`.
App'en vil starte opp en container med siste versjon av postgres.

For å kjøre opp postgres containern så kjører man `docker-compose up`
For å ta ned containern så kjører man `docker-compose down`
For å slette volumen `docker-compose down -v`

### Kjøring med brev
Vanlgvis kjøres appen opp med mockede versjoner av `familie-brev` og `familie-dokument`. 
For å kjøre opp med lokale versjoner av disse appene kan en kommentere ut `mock-brev` og `mock-dokument` i `ApplicationLocalPostgres`.
Deretter kan en kjøre opp appen `familie-brev` i brancen `klage-config` og appen `familie-dokument` i brancen `mocket-auth-server`.
I `familie-dokument` må en kjøre `DevLauncherMedMockServer`. 

### Dummy-svar fra kabal ved lokal kjøring
Lokalt finnes ingen kafka, så for å kunne generere et svar fra kabal kan man bruke endepunktene i `TestHendelseController`. 
* `POST mot http://localhost:8094/api/test/kabal/<behandling_id>/dummy`, med curl eller i postman (uten Authorization-header for å unngå at man prøver å sende inn det som saksbehandler)

## Produksjonssetting
Applikasjonen vil deployes til produksjon ved ny commit på master-branchen. Det er dermed tilstrekkelig å merge PR for å trigge produksjonsbygget.

## Roller
Testbrukeren som opprettes i IDA må ha minst en av følgende roller:
- 0000-GA-Enslig-Forsorger-Beslutter
- 0000-GA-Enslig-Forsorger-Saksbehandler

## Testdata
- Registering av arbeidssøker - https://arbeidssokerregistrering.dev.nav.no/

### Koble til database i preprod:
[Oppskrift på databasetilkobling](https://github.com/navikt/familie/blob/cb403dbf0e7e5af2f5b0d8168d89dae87ce318c4/doc/utvikling/gcp/gcp_kikke_i_databasen.md)

For å koble til preprod kan du kjøre kommandoene:
1. `gcloud auth login`
2. `gcp-db teamfamilie-dev-ae07 familie-klage`
3. Url: `jdbc:postgresql://localhost:5432/familie-klage`
4. Brukernavn: `fornavn.etternavn@nav.no` som brukernavn
5. Passord: Lim inn det som ligger i clipboard fra steg 2
6. Har du ikke lagt til deg selv som database-bruker må du gjør [dette først](https://doc.nais.io/persistence/postgres/)

## Ny fagsystemløsning
1. Fagsystemet må implementere et endepunkt for å hente ut alle vedtak
   1. `fagsystemUrl/api/ekstern/vedtak` som returnerer `Ressurs<List<FagsystemVedtak>>`
2. Må legge inn fagsystemet i inbound og outbound i `preprod.yaml` og `prod.yaml`
3. For å opprette en klage må fagsystemet kalle på `api/ekstern/behandling/opprett` med `OpprettKlagebehandlingRequest`.  
4. For å hente ut alle klager må fagsystemet kalle på `api/ekstern/behandling/{fagsystem}?eksternFagsakIder={eksternFagsakId}`

# Henvendelser


Spørsmål knyttet til koden eller prosjektet kan rettes til:

* Viktor Grøndalen Solberg, `viktor.grondalen.solberg@nav.no`
* Eirik Årseth `eirik.arseth@nav.no`

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-familie.
