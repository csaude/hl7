import OpenMRSAPIStep from "./OpenMRSAPIStep";
import OpenMRSDatabaseStep from "./OpenMRSDatabaseStep";
import SecurityStep from "./SecurityStep";
import SuccessStep from "./SuccessStep";
import WebappAuthenticationStep from "./WebappAuthenticationStep";
import WebappFolderStep from "./WebappFolderStep";

const steps = [
  { name: "Pasta", component: WebappFolderStep },
  { name: "Segurança", component: SecurityStep },
  { name: "API OpenMRS", component: OpenMRSAPIStep },
  { name: "Base de dados OpenMRS", component: OpenMRSDatabaseStep },
  { name: "Autenticação", component: WebappAuthenticationStep },
  { name: "Configurado com sucesso", component: SuccessStep },
];

export { steps };
