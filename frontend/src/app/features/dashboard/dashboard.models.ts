export interface Kpi {
  code: string;
  label: string;
  value: number;
}

export interface QuickAction {
  label: string;
  link: string;
}

// Un indicateur enrichi pour l'affichage : lien vers la page qui détaille le chiffre,
// et drapeau « à traiter » quand le chiffre appelle une action de l'utilisateur.
export interface KpiView extends Kpi {
  link: string | null;
  queryParams: Record<string, string> | null;
  needsAttention: boolean;
}
