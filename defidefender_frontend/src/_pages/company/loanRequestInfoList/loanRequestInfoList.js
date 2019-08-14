import React, { Component } from 'react'
import { withStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container'
import Table from '@material-ui/core/Table'
import TableBody from '@material-ui/core/TableBody'
import TableCell from '@material-ui/core/TableCell'
import TableHead from '@material-ui/core/TableHead'
import TableRow from '@material-ui/core/TableRow'
import Paper from '@material-ui/core/Paper'
import { Button } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles'
import Tick from '@material-ui/icons/DoneOutline'
import Collapse from '@material-ui/core/Collapse';

const useStyles = makeStyles(theme => ({
    title: {
        textAlign: 'center'
    },
    content: {
        flexGrow: 1,
        height: '100vh',
        overflow: 'auto',
    },
    container: {
        paddingTop: theme.spacing(4),
        paddingBottom: theme.spacing(4),
    },
    paper: {
        padding: theme.spacing(2),
        display: 'flex',
        overflow: 'auto',
        flexDirection: 'column',
    },
    fixedHeight: {
        height: 240,
    },
    button: {
        margin: theme.spacing(1),
    },
    leftIcon: {
        marginRight: theme.spacing(2),
    },
    rightIcon: {
        marginLeft: theme.spacing(1.5),
    },
    polygon: {
        fill: theme.palette.common.white,
        stroke: theme.palette.divider,
        strokeWidth: 1,
    },
}))

// 个人借贷请求列表
function LoanRequestInfoList(props) {
    
    const [buttonOpen, setButtonOpen] = React.useState(true);

    const handleButtonClick = ()=>{
        setButtonOpen(prev => !prev)
    }

    const oppo = (flag) => {
        console.log("123")
        console.log(flag);
        flag = !flag
    }

    // ipfs_hash可直接跳转到IPFS上显示
    const createData = (weid, hash, flag) =>{
        return { weid, hash, flag}
    }
    
    const rows = [
        createData(`0xc83b2cf766d3165acc2fc916`, `QmSsw6EcnwEiTT9c4rnAGeSENvsJMepNHmbrgi2S9bXNJr`,false),
        createData(`0xc83b2cf766d3165acc2fc916`, `QmSsw6EcnwEiTT9c4rnAGeSENvsJMepNHmbrgi2S9bXNJr`,false),
        createData(`0xc83b2cf766d3165acc2fc916`, `QmSsw6EcnwEiTT9c4rnAGeSENvsJMepNHmbrgi2S9bXNJr`,false),
        createData(`0xc83b2cf766d3165acc2fc916`, `QmSsw6EcnwEiTT9c4rnAGeSENvsJMepNHmbrgi2S9bXNJr`,false),
        createData(`0xc83b2cf766d3165acc2fc916`, `QmSsw6EcnwEiTT9c4rnAGeSENvsJMepNHmbrgi2S9bXNJr`,false)
    ]

    const classes = useStyles()
    return (
        <div>
            <h3 className={classes.title}>借贷请求列表</h3>
            <Container maxWidth="lg" className={classes.container}>
                <Paper className={classes.root}>
                    <Table className={classes.table}>
                        <TableHead>
                            <TableRow>
                                <TableCell align="center">WeID</TableCell>
                                <TableCell align="center">凭证 Hash</TableCell>
                                <TableCell align="center">查询</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>        
                            {rows.map((row,index) => (
                                <TableRow key = {index}>
                                    <TableCell align="center">{row.weid}</TableCell>
                                    <TableCell align="center">
                                        <Button variant="contained" color="primary" className={classes.button} onClick={(index)=>oppo()} >
                                            验证凭证Hash
                                        </Button>
                                    </TableCell>
                                    <TableCell align="center">                                 
                                        <Button variant="outlined" color="primary" className={classes.button} >
                                            查询多方投资借贷数据
                                        </Button>
                                        <Button variant="outlined" color="primary" className={classes.button} >
                                            查询借贷不良信用记录
                                        </Button>
                                    </TableCell>                                  
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Paper>
            </Container>
        </div>
    )
    
}

const LoanRequestInfoListWithStyles = withStyles(useStyles)(LoanRequestInfoList)

export { LoanRequestInfoListWithStyles as LoanRequestInfoList };