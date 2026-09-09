import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';

const RegistroAcesso = sequelize.define('RegistroAcesso', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  usuarioId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  tipo: {
    type: DataTypes.ENUM('ENTRADA', 'SAIDA'),
    allowNull: false,
  },
  registradoPorId: {
    type: DataTypes.INTEGER,
    allowNull: true,
  },
  condominioId: {
    type: DataTypes.STRING(50),
    allowNull: true,
  },
  nomeSnapshot: {
    type: DataTypes.STRING(150),
    allowNull: true,
  },
  perfilSnapshot: {
    type: DataTypes.STRING(50),
    allowNull: true,
  },
}, {
  tableName: 'registros_acesso',
  timestamps: true,
  updatedAt: false,
  createdAt: 'createdAt',
});

export default RegistroAcesso;
