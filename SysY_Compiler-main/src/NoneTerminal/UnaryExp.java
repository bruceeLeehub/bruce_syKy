package NoneTerminal;

import MyError.Error;
import ParcelType.My_Int;
import Tables.Code;
import Tables.CodeType;
import Table.SymTable;
import Table.TableEntry;
import Tables.Table;
import WordAnalyse.IdentifySymbol;
import WordAnalyse.RegKey;
import WordAnalyse.Symbol;

import java.util.ArrayList;

public class UnaryExp {
    public static String name = "<UnaryExp>";

    // choose one from three
    private PrimaryExp primaryExp;
    private Ident ident;
    private ArrayList<Exp> funcRParamList;
    private UnaryOp unaryOp;
    private UnaryExp unaryExp;

    public UnaryExp() {
        this.primaryExp = null;

        this.ident = null;
        this.funcRParamList = new ArrayList<>();

        this.unaryOp = null;
        this.unaryExp = null;
    }

    public void setPrimaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
    }

    public void setIdent(Ident ident) {
        this.ident = ident;
    }

    public void setUnaryOp(UnaryOp unaryOp) {
        this.unaryOp = unaryOp;
    }

    public void setUnaryExp(UnaryExp unaryExp) {
        this.unaryExp = unaryExp;
    }

    public void addFuncRParam(Exp exp) {
        this.funcRParamList.add(exp);
    }

    public void genCode(My_Int value) {
        if (value != null) {      // is const, you need to calculate it right now
            if (primaryExp != null)
                primaryExp.genCode(value);
            /* there will not be func call when there is a constExp
            else if (ident != null)
                value.myInt = SymTable.getValueOfIdent(ident);
             */
            else if (unaryOp != null) {
                unaryExp.genCode(value);
                if (unaryOp.getOp().equals(RegKey.MINU))
                    value.my_Int = -value.my_Int;
            }
        } else {          // not a const you need to get it when running program
            if (primaryExp != null)
                primaryExp.genCode(null);
            else if (ident != null) {    // func call
                Code.addCode(CodeType.MKS, Table.getFuncTableEntry(ident.getId_Name()).get_Ref());
                for (Exp exp : funcRParamList) {
                    exp.genCode(null);
                }
                Code.addCode(CodeType.CAL);
            } else {
                unaryExp.genCode(null);
                if (unaryOp.getOp().equals(RegKey.MINU))
                    Code.addCode(CodeType.MUS);
                else if (unaryOp.getOp().equals(RegKey.NOT))
                    Code.addCode(CodeType.NOT);
            }
        }
    }

    public static UnaryExp analyse(IdentifySymbol identifySymbol) {
        boolean judge = true;
        UnaryExp unaryExp = new UnaryExp(); // ast Tree node
        Symbol sym = identifySymbol.get_CurrentSym();
        if (sym.getRegKey() == RegKey.LPARENT) {    // '(' Exp ')'
            unaryExp.setPrimaryExp(PrimaryExp.analyse(identifySymbol));
        } else if (sym.getRegKey() == RegKey.PLUS ||
                sym.getRegKey() == RegKey.MINU ||
                sym.getRegKey() == RegKey.NOT) {

            unaryExp.setUnaryOp(UnaryOp.analyse(identifySymbol));
            unaryExp.setUnaryExp(UnaryExp.analyse(identifySymbol));
        } else if (sym.getRegKey() == RegKey.IDENFR) {
            Symbol identSym = sym;
            TableEntry tmpEntry;
            sym = identifySymbol.getASymbol();

            if (sym.getRegKey() == RegKey.LPARENT) { // Ident '(' [ FuncRParams ] ')'
                unaryExp.setIdent(new Ident(identSym.get_IdentName()));
                My_Int numOfParamsActually = new My_Int();
                // ERROR: check name undefined -- type c
                Error.checkNameUndefined(true, identSym);
                // checking RParams type
                tmpEntry = SymTable.get_SymByNameInAllTable(true, identSym.get_IdentName());
                if (FuncRParams.TypeCheck) {
                    FuncRParams.tbEntryModel.add(SymTable.createTableEntryModel(tmpEntry, 0));
                    if (LVal.inDims == 0)
                        FuncRParams.TypeCheck = false;
                }
                if (identifySymbol.getASymbol().getRegKey() != RegKey.RPARENT) {
                    judge &= FuncRParams.analyse(identifySymbol, numOfParamsActually, unaryExp);
                    if (judge) {
                        // ERROR -- j: ')' needed
                        if (identifySymbol.get_CurrentSym().getRegKey() != RegKey.RPARENT)
                            Error.addErrorOutPut(identifySymbol.get_PreSym().getRow_Idx() + " j");
                        else identifySymbol.getASymbol();
                    }
                } else {
                    identifySymbol.getASymbol();
                }
                // ERROR: check is number of param matches
                Error.checkParamNumMatched(identSym, numOfParamsActually.my_Int);
            } else {        // PrimaryExp
                identifySymbol.spitSym(1);
                unaryExp.setPrimaryExp(PrimaryExp.analyse(identifySymbol));
            }

        } else if (sym.getRegKey() == RegKey.INTCON) {
            unaryExp.setPrimaryExp(PrimaryExp.analyse(identifySymbol));
        }

        if (judge) {
            identifySymbol.addStr(name);
        }

        return unaryExp;
    }

    public static boolean isMyFirst(Symbol sym) {
        if (sym.getRegKey() == RegKey.INTCON ||
                UnaryOp.isMyFirst(sym) || PrimaryExp.isMyFirst(sym)) {
            return true;
        }
        return false;
    }
}
