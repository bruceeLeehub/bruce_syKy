package NoneTerminal;

import MyError.Error;
import ParcelType.My_DataType;
import ParcelType.MyString;
import Table.DataType;
import Table.SymTable;
import Table.TableEntry;
import Tables.*;
import WordAnalyse.IdentifySymbol;
import WordAnalyse.RegKey;
import WordAnalyse.Symbol;

import java.util.ArrayList;

public class FuncDef {
    public static String name = "<FuncDef>";
    public static boolean isInFuncDef = false;
    public static boolean funcBlockST = false;
    public static boolean haveRetVal = false;
    public static boolean isFunEnd = false;

    private FuncType funcType;
    private Ident ident;
    private ArrayList<FuncFParam> funcFParamList;
    private Block block;

    public FuncDef() {
        this.funcType = null;
        this.ident = null;
        this.funcFParamList = new ArrayList<>();
        this.block = null;
    }

    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }

    public void setIdent(Ident ident) {
        this.ident = ident;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void addFuncFParam(FuncFParam funcFParam) {
        this.funcFParamList.add(funcFParam);
    }

    public void genCode() {
        Typ typ = Typ.INT_TYP;
        int funcRef = ProgTable.insertProg_Entry(ident.getIdentName(), funcFParamList.size());
        if (this.funcType.getRegKey().equals(RegKey.VOIDTK))
            typ = Typ.VOID_TYP;

        Table.addTeToCurrentTable(ident.getIdentName(), Obj.FUNC_OBJ, typ, 0,
                funcRef, Table.getCurLayer(), Code.get_NextFreeRoom());
        Table.createANewLayer();
        for (FuncFParam para : funcFParamList) {
            para.genCode();
        }
        this.block.genCode(ident.getIdentName(), Table.getFuncTableEntry(ident.getIdentName()));
    }

    public static FuncDef analyse(IdentifySymbol identifySymbol) {
        Symbol sym;
        boolean judge = true;
        MyString identName = new MyString();
        My_DataType dataType = new My_DataType();
        ArrayList<TableEntry> paramList = new ArrayList<>();
        funcBlockST = true;
        isInFuncDef = true;
        isFunEnd = false;
        FuncDef funcDef = new FuncDef();    // ast tree node

        funcDef.setFuncType(FuncType.analyse(identifySymbol, dataType));

        if (judge) funcDef.setIdent(Ident.analyse(identifySymbol, identName));
        // ERROR: name Duplicated define -- type b
        Error.checkIfDupDef(true, identifySymbol.getPreSym());
        // record does it have a return value
        haveRetVal = dataType.dataType == DataType.INT_DATA;
        // create symTable stack of the following func
        SymTable.createNewTable();

        if (judge) {
            sym = identifySymbol.getCurSym();
            judge &= sym.getRegKey() == RegKey.LPARENT;
            if (judge) {
                sym = identifySymbol.getASymbol();
                if (sym.getRegKey() == RegKey.RPARENT || sym.getRegKey() == RegKey.LBRACE) {
                    if (sym.getRegKey() == RegKey.LBRACE) {
                        // ERROR -- j: ')' needed
                        if (identifySymbol.getCurSym().getRegKey() != RegKey.RPARENT)
                            Error.addErrorOutPut(identifySymbol.getPreSym().getRow_Idx() + " j");
                    } else {
                        identifySymbol.getASymbol();
                    }
                    // insert funcDef into previous stmTable
                    if (CompUnit.isNameDupDef == false)
                        SymTable.insertTabEntryIntoPreTab(true, identName.string, false, dataType.dataType, 0, paramList);
                    else CompUnit.isNameDupDef = false;
                    funcDef.setBlock(Block.analyse(identifySymbol));
                    // ERROR -- g: func have return value don't have return stmt in the end
                    if (FuncDef.haveRetVal == true && Block.hasReturnStmt == false)
                        Error.addErrorOutPut(identifySymbol.getPreSym().getRow_Idx() + " g");
                } else {
                    judge &= FuncFParams.analyse(identifySymbol, paramList, funcDef);
                    // ERROR -- j: ')' needed
                    if (identifySymbol.getCurSym().getRegKey() != RegKey.RPARENT)
                        Error.addErrorOutPut(identifySymbol.getPreSym().getRow_Idx() + " j");
                    else identifySymbol.getASymbol();
                    // insert funcDef into previous stmTable
                    if (CompUnit.isNameDupDef == false)
                        SymTable.insertTabEntryIntoPreTab(true, identName.string, false, dataType.dataType, 0, paramList);
                    else CompUnit.isNameDupDef = false;
                    funcDef.setBlock(Block.analyse(identifySymbol));
                    // ERROR -- g: func have return value don't have return stmt in the end
                    if (FuncDef.haveRetVal == true && Block.hasReturnStmt == false)
                        Error.addErrorOutPut(identifySymbol.getPreSym().getRow_Idx() + " g");

                }

            }
        }

        if (judge) identifySymbol.addStr(name);

        isInFuncDef = false;
        // remove the current function table stack
        // SymTable.popOutterTable();
        // Block has done the remove job even when a table is built here
        return funcDef;
    }
}
