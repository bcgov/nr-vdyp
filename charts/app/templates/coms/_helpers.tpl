{{/*
Expand the name of the chart.
*/}}
{{- define "coms.name" -}}
{{- printf "coms" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "coms.fullname" -}}
{{- $componentName := include "coms.name" .  }}
{{- printf "%s-%s" .Release.Name $componentName | trunc 63 | trimSuffix "-" }}
{{- end }}


{{/*
Common labels
*/}}
{{- define "coms.labels" -}}
{{ include "coms.selectorLabels" . }}
{{- if .Values.global.tag }}
app.kubernetes.io/image-version: {{ .Values.global.tag | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/short-name: {{ include "coms.name" . }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "coms.selectorLabels" -}}
app.kubernetes.io/name: {{ include "coms.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


