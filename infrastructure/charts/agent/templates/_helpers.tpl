{{- define "cors" }}
    {{- if .Values.cors.enabled -}}
    - name: cors
      enable: true
      {{- if .Values.cors.allow_origins }}
      config:
        allow_origins: {{ .Values.cors.allow_origins | quote }}
      {{- end}}
    {{- end -}}
{{- end }}
{{- define "consumer-restriction" }}
    - name: consumer-restriction
      enable: true
      config:
        whitelist:
        {{- range .Values.consumers }}
          -  {{ regexReplaceAll "-" $.Release.Name "_" }}_{{ regexReplaceAll "-" . "_" | lower }}
        {{- end }}
{{- end }}
